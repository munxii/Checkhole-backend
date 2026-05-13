package com.example.SmartHelmet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "sensor_readings")
public class SensorReading {

    @Id
    private String id;

    @Indexed
    private String pipeId;

    @Indexed
    private LocalDateTime timestamp;

    private double displacement;
    private double pressure;
    private double moisture;
    private double vibration;

    /** Acoustic resonance peak frequency (Hz). Populated by gateway ingestion only. */
    private Double peak;

    /** Acoustic resonance amplitude (0.0~1.0). Populated by gateway ingestion only. */
    private Double amp;

    /** Edge-AI (IsolationForest) anomaly probability (0.0~1.0). Null if AI not available. */
    private Double anomalyScore;
}
