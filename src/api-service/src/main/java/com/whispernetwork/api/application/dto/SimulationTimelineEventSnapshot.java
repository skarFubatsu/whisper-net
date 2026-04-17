package com.whispernetwork.api.application.dto;

import java.time.Instant;
import java.util.List;

/**
 * Application-native event model used for timeline queries and SSE.
 */
public record SimulationTimelineEventSnapshot(
        String eventType,
        String simulationRunId,
        String networkId,
        Integer tickNumber,
        Integer updatedAgents,
        Integer completedTicks,
        String reason,
        String agentId,
        Double previousOpinionValue,
        Double newOpinionValue,
        Double incomingInfluenceMagnitude,
        List<String> contributingSourceAgentIds,
        Boolean relayedAsIs,
        String relayOriginAgentId,
        Boolean ignoredTrustAndWeight,
        Instant occurredAt) {}
