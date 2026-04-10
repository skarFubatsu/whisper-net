package com.whispernetwork.simulation.core.engine;

import java.util.List;

/**
 * Aggregation output for one target agent.
 */
public record OpinionAggregationResult(
    double newOpinionValue,
    double incomingInfluenceMagnitude,
    boolean relayedAsIs,
    String relayOriginAgentId,
    boolean ignoredTrustAndWeight,
    List<String> contributingSourceAgentIds) {
}
