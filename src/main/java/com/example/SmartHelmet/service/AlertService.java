package com.example.SmartHelmet.service;

import com.example.SmartHelmet.dto.Alert;
import com.example.SmartHelmet.dto.Member;
import com.example.SmartHelmet.dto.Pipe;
import com.example.SmartHelmet.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private final RegionAccessService regionAccessService;

    public List<Alert> search(Member current, String pipeId, Pipe.Status status) {
        Set<String> visible = regionAccessService.visibleRegionIds(current);
        if (visible.isEmpty()) return List.of();

        List<Alert> base;
        if (pipeId != null) {
            base = alertRepository.findByPipeIdOrderByCreatedAtDesc(pipeId).stream()
                    .filter(a -> a.getRegionId() != null && visible.contains(a.getRegionId()))
                    .toList();
        } else {
            base = alertRepository.findByRegionIdInOrderByCreatedAtDesc(visible);
        }

        if (status == null) return base;
        return base.stream().filter(a -> a.getStatus() == status).toList();
    }
}
