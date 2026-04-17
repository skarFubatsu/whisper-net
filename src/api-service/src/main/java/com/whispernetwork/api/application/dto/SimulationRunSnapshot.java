package com.whispernetwork.api.application.dto;

import java.time.Instant;

/**
 * Application-native simulation run read model.
 */
public record SimulationRunSnapshot(
        String runId,
        String networkId,
        int networkVersionNumber,
        String status,
        int completedTicks,
        int requestedTicks,
        String failureMessage,
        Instant createdAt,
        Instant updatedAt) {}
