package com.example.SmartHelmet.service;

import com.example.SmartHelmet.dto.Pipe;
import com.example.SmartHelmet.repository.PipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PipeService {

    private final PipeRepository pipeRepository;

    public List<Pipe> search(String region, Pipe.Status status) {
        return pipeRepository.findAll().stream()
                .filter(p -> region == null || region.equals(p.getRegion()))
                .filter(p -> status == null || status == p.getStatus())
                .toList();
    }

    public Pipe findById(String id) {
        return pipeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 파이프를 찾을 수 없습니다: " + id));
    }
}
