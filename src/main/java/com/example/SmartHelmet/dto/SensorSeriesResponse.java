package com.example.SmartHelmet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensorSeriesResponse {

    private String pipeId;
    private List<Point> points;

    public record Point(
            LocalDateTime t,
            double displacement,
            double pressure,
            double moisture,
            double vibration
    ) {
        public static Point from(SensorReading r) {
            return new Point(
                    r.getTimestamp(),
                    r.getDisplacement(),
                    r.getPressure(),
                    r.getMoisture(),
                    r.getVibration()
            );
        }
    }

    public static SensorSeriesResponse from(String pipeId, List<SensorReading> readings) {
        return SensorSeriesResponse.builder()
                .pipeId(pipeId)
                .points(readings.stream().map(Point::from).toList())
                .build();
    }
}
