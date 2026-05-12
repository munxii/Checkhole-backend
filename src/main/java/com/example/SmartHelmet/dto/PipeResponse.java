package com.example.SmartHelmet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PipeResponse {
    private String id;
    private String name;
    private String region;
    private String regionId;
    private String address;
    private double lat;
    private double lng;
    private String status;
    private double sensorValue;
    private LocalDate installedAt;
    private LocalDateTime updatedAt;

    public static PipeResponse from(Pipe pipe) {
        return PipeResponse.builder()
                .id(pipe.getId())
                .name(pipe.getName())
                .region(pipe.getRegion())
                .regionId(pipe.getRegionId())
                .address(pipe.getAddress())
                .lat(pipe.getLat())
                .lng(pipe.getLng())
                .status(pipe.getStatus() == null ? null : pipe.getStatus().name().toLowerCase())
                .sensorValue(pipe.getSensorValue())
                .installedAt(pipe.getInstalledAt())
                .updatedAt(pipe.getUpdatedAt())
                .build();
    }
}
