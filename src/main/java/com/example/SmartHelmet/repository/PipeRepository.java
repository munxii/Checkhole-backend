package com.example.SmartHelmet.repository;

import com.example.SmartHelmet.dto.Pipe;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PipeRepository extends MongoRepository<Pipe, String> {
    List<Pipe> findByRegion(String region);
    List<Pipe> findByStatus(Pipe.Status status);
}
