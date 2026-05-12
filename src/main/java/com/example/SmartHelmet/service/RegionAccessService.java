package com.example.SmartHelmet.service;

import com.example.SmartHelmet.dto.Member;
import com.example.SmartHelmet.dto.Pipe;
import com.example.SmartHelmet.dto.Region;
import com.example.SmartHelmet.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RegionAccessService {

    private final RegionRepository regionRepository;

    /**
     * Region IDs visible to this member.
     * - CENTRAL → all regions
     * - METRO   → own region + child DISTRICTs
     * - DISTRICT/FIELD → own region only
     * - CITIZEN or null → empty (no access)
     */
    public Set<String> visibleRegionIds(Member member) {
        if (member == null || member.getRole() == null) return Set.of();
        return switch (member.getRole()) {
            case CENTRAL -> regionRepository.findAll().stream()
                    .map(Region::getId)
                    .collect(Collectors.toUnmodifiableSet());
            case METRO -> {
                if (member.getPrimaryRegionId() == null) yield Set.of();
                Set<String> ids = new HashSet<>();
                ids.add(member.getPrimaryRegionId());
                regionRepository.findByParentId(member.getPrimaryRegionId())
                        .forEach(r -> ids.add(r.getId()));
                yield Set.copyOf(ids);
            }
            case DISTRICT, FIELD -> member.getPrimaryRegionId() == null
                    ? Set.of()
                    : Set.of(member.getPrimaryRegionId());
            case CITIZEN -> Set.of();
        };
    }

    public boolean canAccessPipe(Member member, Pipe pipe) {
        if (pipe == null || pipe.getRegionId() == null) return false;
        return visibleRegionIds(member).contains(pipe.getRegionId());
    }

    /** Display label for sidebar / login response. */
    public String displayName(Member member) {
        if (member == null || member.getRole() == null) return "";
        Member.Role role = member.getRole();
        if (role == Member.Role.CENTRAL) return "전국";
        if (role == Member.Role.CITIZEN) return "시민";
        if (member.getPrimaryRegionId() == null) return "";

        Region region = regionRepository.findById(member.getPrimaryRegionId()).orElse(null);
        if (region == null) return "";

        if (region.getLevel() == Region.Level.METRO) return region.getName();
        if (region.getParentId() != null) {
            Region parent = regionRepository.findById(region.getParentId()).orElse(null);
            if (parent != null) return parent.getName() + " " + region.getName();
        }
        return region.getName();
    }
}
