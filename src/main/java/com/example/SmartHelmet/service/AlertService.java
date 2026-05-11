package com.example.SmartHelmet.service;

import com.example.SmartHelmet.dto.Alert;
import com.example.SmartHelmet.dto.Pipe;
import com.example.SmartHelmet.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;

    public List<Alert> search(String pipeId, Pipe.Status status) {
        List<Alert> base = (pipeId == null)
                ? alertRepository.findAllByOrderByCreatedAtDesc()
                : alertRepository.findByPipeIdOrderByCreatedAtDesc(pipeId);

        if (status == null) return base;
        return base.stream().filter(a -> a.getStatus() == status).toList();
    }
}
