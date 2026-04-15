package com.whispernetwork.shared.dto;

/**
 * Shared timeline/history row contract for relational query results.
 */
public record SimulationHistoryEntry(
        long eventId,
        String simulationRunId,
        String networkId,
        String eventType,
        Integer tickNumber,
        Integer updatedAgents,
        String actorId,
        String clientRequestId,
        String reason,
        Integer completedTicks,
        long occurredAtEpochMillis,
        long createdAtEpochMillis) {}
