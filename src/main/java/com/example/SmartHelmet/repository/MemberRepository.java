package com.example.SmartHelmet.repository;

import com.example.SmartHelmet.dto.Member;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface MemberRepository extends MongoRepository<Member, String> {
    Optional<Member> findByUsername(String username);
    boolean existsByUsername(String username);
}
