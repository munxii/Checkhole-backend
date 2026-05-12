package com.example.SmartHelmet.controller;

import com.example.SmartHelmet.dto.AlertResponse;
import com.example.SmartHelmet.dto.GatewayReadingRequest;
import com.example.SmartHelmet.service.GatewayIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/gateway")
@RequiredArgsConstructor
public class GatewayController {

    private final GatewayIngestionService gatewayIngestionService;

    @Value("${gateway.api-key}")
    private String expectedKey;

    @PostMapping("/sensor-reading")
    public ResponseEntity<?> ingest(
            @RequestHeader(value = "X-Gateway-Key", required = false) String providedKey,
            @RequestBody GatewayReadingRequest req
    ) {
        if (providedKey == null || !providedKey.equals(expectedKey)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid gateway key");
        }

        AlertResponse alert = gatewayIngestionService.ingest(req);
        if (alert == null) {
            return ResponseEntity.ok(Map.of(
                    "status", "ok",
                    "judgement", "NORMAL",
                    "alertCreated", false
            ));
        }
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "judgement", alert.getStatus(),
                "alertCreated", true,
                "alert", alert
        ));
    }
}
