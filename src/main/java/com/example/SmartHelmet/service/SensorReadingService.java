package com.example.SmartHelmet.service;

import com.example.SmartHelmet.dto.SensorReading;
import com.example.SmartHelmet.dto.SensorSeriesResponse;
import com.example.SmartHelmet.repository.SensorReadingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SensorReadingService {

    private final SensorReadingRepository sensorReadingRepository;

    public SensorSeriesResponse getSeries(String pipeId, int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        List<SensorReading> readings = sensorReadingRepository
                .findByPipeIdAndTimestampGreaterThanEqualOrderByTimestampAsc(pipeId, since);
        return SensorSeriesResponse.from(pipeId, readings);
    }
}
