package com.example.SmartHelmet.service;

import com.example.SmartHelmet.dto.Alert;
import com.example.SmartHelmet.dto.AlertResponse;
import com.example.SmartHelmet.dto.GatewayReadingRequest;
import com.example.SmartHelmet.dto.Pipe;
import com.example.SmartHelmet.dto.SensorReading;
import com.example.SmartHelmet.repository.AlertRepository;
import com.example.SmartHelmet.repository.PipeRepository;
import com.example.SmartHelmet.repository.SensorReadingRepository;
import com.example.SmartHelmet.websocket.AlertEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

@Slf4j
@Service
@RequiredArgsConstructor
public class GatewayIngestionService {

    private static final double CAUTION_THRESHOLD = 0.4;
    private static final double DANGER_THRESHOLD = 0.7;

    private final PipeRepository pipeRepository;
    private final SensorReadingRepository sensorReadingRepository;
    private final AlertRepository alertRepository;
    private final AlertEventPublisher alertEventPublisher;

    public AlertResponse ingest(GatewayReadingRequest req) {
        if (req.deviceId() == null || req.deviceId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "deviceId is required");
        }
        if (req.amp() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amp is required");
        }

        Pipe pipe = pipeRepository.findByDeviceId(req.deviceId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "no pipe mapped to deviceId: " + req.deviceId()));

        double amp = req.amp();
        double peak = req.peak() != null ? req.peak() : 0.0;
        Double anomalyScore = req.anomalyScore();
        LocalDateTime ts = parseTimestamp(req.receivedAt());

        // Judgement source: anomaly_score (AI) takes precedence; fall back to amp.
        double judgementValue = anomalyScore != null ? anomalyScore : amp;
        Pipe.Status newStatus;
        if (judgementValue >= DANGER_THRESHOLD) newStatus = Pipe.Status.DANGER;
        else if (judgementValue >= CAUTION_THRESHOLD) newStatus = Pipe.Status.CAUTION;
        else newStatus = Pipe.Status.NORMAL;

        // Persist sensor reading (peak/amp/anomalyScore populated as available)
        sensorReadingRepository.save(SensorReading.builder()
                .pipeId(pipe.getId())
                .timestamp(ts)
                .peak(peak)
                .amp(amp)
                .anomalyScore(anomalyScore)
                .build());

        // Update pipe summary
        pipe.setStatus(newStatus);
        pipe.setSensorValue(Math.round(amp * 1000.0) / 10.0); // amp*100, 1 decimal
        pipe.setUpdatedAt(ts);
        pipeRepository.save(pipe);

        log.info("[gateway] ingest deviceId={} amp={} anomalyScore={} → {} (pipe {})",
                req.deviceId(), amp, anomalyScore, newStatus, pipe.getId());

        // No alert for NORMAL — only persist reading + update pipe
        if (newStatus == Pipe.Status.NORMAL) return null;

        String label = newStatus == Pipe.Status.DANGER ? "[위험]" : "[주의]";
        int score = (int) Math.round(judgementValue * 100);
        String message;
        if (anomalyScore != null) {
            message = String.format(
                    "%s AI 추론 기반 빈 공간 가능성 %d%% (peak: %.0f Hz, amp: %.2f)",
                    label, score, peak, amp);
        } else {
            message = String.format(
                    "%s 공명 진폭 이상 — 빈 공간 가능성 %d%% (peak: %.0f Hz, amp: %.2f)",
                    label, score, peak, amp);
        }

        Alert alert = Alert.builder()
                .pipeId(pipe.getId())
                .pipeName(pipe.getName())
                .regionId(pipe.getRegionId())
                .status(newStatus)
                .message(message)
                .createdAt(ts)
                .build();
        Alert saved = alertRepository.save(alert);

        AlertResponse response = AlertResponse.from(saved);
        alertEventPublisher.publish(response);
        return response;
    }

    private LocalDateTime parseTimestamp(String s) {
        if (s == null || s.isBlank()) return LocalDateTime.now();
        try {
            return OffsetDateTime.parse(s).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(s);
            } catch (DateTimeParseException ignored2) {
                log.warn("[gateway] unparseable receivedAt='{}' — using server clock", s);
                return LocalDateTime.now();
            }
        }
    }
}
