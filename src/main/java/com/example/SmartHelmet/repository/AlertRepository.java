package com.example.SmartHelmet.repository;

import com.example.SmartHelmet.dto.Alert;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;

public interface AlertRepository extends MongoRepository<Alert, String> {
    List<Alert> findByPipeIdOrderByCreatedAtDesc(String pipeId);
    List<Alert> findAllByOrderByCreatedAtDesc();
    List<Alert> findByRegionIdInOrderByCreatedAtDesc(Collection<String> regionIds);
}
