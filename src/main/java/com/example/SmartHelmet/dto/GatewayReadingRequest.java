package com.example.SmartHelmet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payload from the Raspberry Pi gateway after demodulating a LoRa packet.
 *
 * - peak:           acoustic resonance peak frequency (Hz)
 * - amp:            acoustic resonance amplitude (0.0~1.0) — used for status if anomalyScore absent
 * - anomalyScore:   edge-AI (IsolationForest) anomaly probability (0.0~1.0). When present, takes
 *                   precedence over amp for status judgement.
 * - lat/lng:        device location at time of measurement (informational)
 * - receivedAt:     optional ISO-8601 timestamp; server clock used if absent
 */
public record GatewayReadingRequest(
        String deviceId,
        Double peak,
        Double amp,
        @JsonProperty("anomaly_score") Double anomalyScore,
        Double lat,
        Double lng,
        String receivedAt,
        Integer batteryLevel
) {}
