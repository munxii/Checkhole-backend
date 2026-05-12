package com.example.SmartHelmet.service;

import com.example.SmartHelmet.dto.Alert;
import com.example.SmartHelmet.dto.AlertResponse;
import com.example.SmartHelmet.dto.Member;
import com.example.SmartHelmet.dto.Pipe;
import com.example.SmartHelmet.repository.AlertRepository;
import com.example.SmartHelmet.websocket.AlertEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private final RegionAccessService regionAccessService;
    private final PipeService pipeService;
    private final AlertEventPublisher alertEventPublisher;

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

    public AlertResponse create(Member current, String pipeId, Pipe.Status status, String message) {
        if (status != Pipe.Status.CAUTION && status != Pipe.Status.DANGER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "status must be CAUTION or DANGER");
        }

        // RBAC: throws 404 if pipe not visible to current user
        Pipe pipe = pipeService.findById(current, pipeId);

        String finalMessage = (message != null && !message.isBlank())
                ? message
                : (status == Pipe.Status.DANGER
                    ? "[위험] " + pipe.getName() + " 센서값 임계치 초과"
                    : "[주의] " + pipe.getName() + " 수치 상승 감지");

        Alert alert = Alert.builder()
                .pipeId(pipe.getId())
                .pipeName(pipe.getName())
                .regionId(pipe.getRegionId())
                .status(status)
                .message(finalMessage)
                .createdAt(LocalDateTime.now())
                .build();

        Alert saved = alertRepository.save(alert);
        AlertResponse response = AlertResponse.from(saved);
        alertEventPublisher.publish(response);
        return response;
    }
}
