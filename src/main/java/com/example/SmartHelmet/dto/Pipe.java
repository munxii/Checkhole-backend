package com.example.SmartHelmet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "pipes")
public class Pipe {

    @Id
    private String id;

    private String name;
    private String region;

    /** Region.id (e.g., "seoul-gangnam") — RBAC scoping key */
    @org.springframework.data.mongodb.core.index.Indexed
    private String regionId;

    private String address;
    private double lat;
    private double lng;

    private Status status;

    private double sensorValue;

    private LocalDate installedAt;

    private LocalDateTime updatedAt;

    public enum Status {
        NORMAL, CAUTION, DANGER
    }
}
