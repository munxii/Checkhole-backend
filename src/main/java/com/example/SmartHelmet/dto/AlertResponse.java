package com.example.SmartHelmet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertResponse {
    private String id;
    private String pipeId;
    private String pipeName;
    private String status;
    private String message;
    private LocalDateTime createdAt;

    public static AlertResponse from(Alert alert) {
        return AlertResponse.builder()
                .id(alert.getId())
                .pipeId(alert.getPipeId())
                .pipeName(alert.getPipeName())
                .status(alert.getStatus() == null ? null : alert.getStatus().name().toLowerCase())
                .message(alert.getMessage())
                .createdAt(alert.getCreatedAt())
                .build();
    }
}
