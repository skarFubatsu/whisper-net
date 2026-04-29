package com.whispernetwork.api.interfaces.http.dto;

import java.time.Instant;

/**
 * HTTP response model for run status endpoint.
 */
public record SimulationRunStatusResponse(
        String simulationRunId,
        String networkId,
        int networkVersionNumber,
        String status,
        int completedTicks,
        int requestedTicks,
        String failureMessage,
        Instant createdAt,
        Instant updatedAt) {}
