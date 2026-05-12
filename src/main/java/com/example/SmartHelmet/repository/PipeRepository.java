package com.example.SmartHelmet.repository;

import com.example.SmartHelmet.dto.Pipe;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PipeRepository extends MongoRepository<Pipe, String> {
    List<Pipe> findByRegion(String region);
    List<Pipe> findByStatus(Pipe.Status status);
    List<Pipe> findByRegionIdIn(Collection<String> regionIds);
    Optional<Pipe> findByDeviceId(String deviceId);
}
