package com.example.SmartHelmet.service;

import com.example.SmartHelmet.dto.Member;
import com.example.SmartHelmet.dto.Pipe;
import com.example.SmartHelmet.repository.PipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PipeService {

    private final PipeRepository pipeRepository;
    private final RegionAccessService regionAccessService;

    public List<Pipe> search(Member current, String region, Pipe.Status status) {
        Set<String> visible = regionAccessService.visibleRegionIds(current);
        if (visible.isEmpty()) return List.of();
        return pipeRepository.findByRegionIdIn(visible).stream()
                .filter(p -> region == null || region.equals(p.getRegion()))
                .filter(p -> status == null || status == p.getStatus())
                .toList();
    }

    public Pipe findById(Member current, String id) {
        Pipe pipe = pipeRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 파이프를 찾을 수 없습니다: " + id));
        if (!regionAccessService.canAccessPipe(current, pipe)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 파이프를 찾을 수 없습니다: " + id);
        }
        return pipe;
    }
}
