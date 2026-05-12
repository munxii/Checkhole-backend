package com.example.SmartHelmet.controller;

import com.example.SmartHelmet.dto.AlertResponse;
import com.example.SmartHelmet.dto.Member;
import com.example.SmartHelmet.dto.Pipe;
import com.example.SmartHelmet.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AlertController {

    private final AlertService alertService;

    public record TriggerRequest(String pipeId, Pipe.Status status, String message) {}

    @GetMapping
    public ResponseEntity<List<AlertResponse>> list(
            @AuthenticationPrincipal Member current,
            @RequestParam(required = false) String pipeId,
            @RequestParam(required = false) Pipe.Status status
    ) {
        List<AlertResponse> body = alertService.search(current, pipeId, status).stream()
                .map(AlertResponse::from)
                .toList();
        return ResponseEntity.ok(body);
    }

    @PostMapping("/trigger")
    @PreAuthorize("hasRole('CENTRAL')")
    public ResponseEntity<AlertResponse> trigger(
            @AuthenticationPrincipal Member current,
            @RequestBody TriggerRequest req
    ) {
        AlertResponse created = alertService.create(current, req.pipeId(), req.status(), req.message());
        return ResponseEntity.ok(created);
    }
}
