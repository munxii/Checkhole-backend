package com.example.SmartHelmet.repository;

import com.example.SmartHelmet.dto.Region;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface RegionRepository extends MongoRepository<Region, String> {
    List<Region> findByParentId(String parentId);
}
