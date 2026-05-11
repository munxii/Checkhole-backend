package com.example.SmartHelmet.repository;

import com.example.SmartHelmet.dto.SensorReading;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface SensorReadingRepository extends MongoRepository<SensorReading, String> {
    List<SensorReading> findByPipeIdAndTimestampGreaterThanEqualOrderByTimestampAsc(
            String pipeId, LocalDateTime since);
}
