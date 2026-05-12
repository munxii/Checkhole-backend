package com.example.SmartHelmet.dto;

/**
 * Payload from the Raspberry Pi gateway after demodulating a LoRa packet.
 *
 * - peak: acoustic resonance peak frequency (Hz)
 * - amp:  acoustic resonance amplitude (0.0~1.0) — used for status judgement
 * - lat/lng: device location at time of measurement (informational, not persisted separately)
 * - receivedAt: optional ISO-8601 timestamp; server clock used if absent
 */
public record GatewayReadingRequest(
        String deviceId,
        Double peak,
        Double amp,
        Double lat,
        Double lng,
        String receivedAt
) {}
