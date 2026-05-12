package com.example.SmartHelmet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "regions")
public class Region {

    @Id
    private String id;

    private String name;

    private Level level;

    @Indexed
    private String parentId;

    private double centerLat;
    private double centerLng;
    private double zoom;

    public enum Level {
        METRO, DISTRICT
    }
}
