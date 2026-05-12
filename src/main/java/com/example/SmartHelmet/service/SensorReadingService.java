package com.example.SmartHelmet.service;

import com.example.SmartHelmet.dto.Member;
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
    private final PipeService pipeService;

    public SensorSeriesResponse getSeries(Member current, String pipeId, int hours) {
        // RBAC check via PipeService — throws 404 if pipe not accessible to current user
        pipeService.findById(current, pipeId);

        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        List<SensorReading> readings = sensorReadingRepository
                .findByPipeIdAndTimestampGreaterThanEqualOrderByTimestampAsc(pipeId, since);
        return SensorSeriesResponse.from(pipeId, readings);
    }
}
