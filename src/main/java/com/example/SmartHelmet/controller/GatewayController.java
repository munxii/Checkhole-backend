package com.example.SmartHelmet.controller;

import com.example.SmartHelmet.dto.AlertResponse;
import com.example.SmartHelmet.dto.GatewayReadingRequest;
import com.example.SmartHelmet.service.GatewayIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Authentication for /api/gateway/** is handled by {@link com.example.SmartHelmet.security.GatewayAuthFilter}
 * (X-Gateway-Key header check). This controller assumes the request is already authorized.
 */
@RestController
@RequestMapping("/api/gateway")
@RequiredArgsConstructor
public class GatewayController {

    private final GatewayIngestionService gatewayIngestionService;

    @PostMapping("/sensor-reading")
    public ResponseEntity<Map<String, Object>> ingest(@RequestBody GatewayReadingRequest req) {
        AlertResponse alert = gatewayIngestionService.ingest(req);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "ok");
        if (alert == null) {
            body.put("judgement", "NORMAL");
            body.put("alertCreated", false);
        } else {
            body.put("judgement", alert.getStatus());
            body.put("alertCreated", true);
            body.put("alert", alert);
        }
        return ResponseEntity.ok(body);
    }
}
