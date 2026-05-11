package com.example.SmartHelmet.controller;

import com.example.SmartHelmet.dto.Pipe;
import com.example.SmartHelmet.dto.PipeResponse;
import com.example.SmartHelmet.dto.SensorSeriesResponse;
import com.example.SmartHelmet.service.PipeService;
import com.example.SmartHelmet.service.SensorReadingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pipes")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class PipeController {

    private final PipeService pipeService;
    private final SensorReadingService sensorReadingService;

    @GetMapping
    public ResponseEntity<List<PipeResponse>> list(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) Pipe.Status status
    ) {
        List<PipeResponse> body = pipeService.search(region, status).stream()
                .map(PipeResponse::from)
                .toList();
        return ResponseEntity.ok(body);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PipeResponse> getOne(@PathVariable String id) {
        return ResponseEntity.ok(PipeResponse.from(pipeService.findById(id)));
    }

    @GetMapping("/{id}/sensors")
    public ResponseEntity<SensorSeriesResponse> sensors(
            @PathVariable String id,
            @RequestParam(defaultValue = "24") int hours
    ) {
        int clamped = Math.max(1, Math.min(168, hours));
        return ResponseEntity.ok(sensorReadingService.getSeries(id, clamped));
    }
}
