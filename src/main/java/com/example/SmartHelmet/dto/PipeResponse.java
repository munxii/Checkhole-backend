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
public class PipeResponse {
    private String id;
    private String name;
    private String region;
    private double lat;
    private double lng;
    private String status;
    private double sensorValue;
    private LocalDateTime updatedAt;

    public static PipeResponse from(Pipe pipe) {
        return PipeResponse.builder()
                .id(pipe.getId())
                .name(pipe.getName())
                .region(pipe.getRegion())
                .lat(pipe.getLat())
                .lng(pipe.getLng())
                .status(pipe.getStatus() == null ? null : pipe.getStatus().name().toLowerCase())
                .sensorValue(pipe.getSensorValue())
                .updatedAt(pipe.getUpdatedAt())
                .build();
    }
}
