package com.whispernetwork.simulation.core.engine;

import java.util.List;

/**
 * Opinion delta emitted for a single updated agent.
 */
public record AgentOpinionUpdate(
    String agentId,
    double previousOpinionValue,
    double newOpinionValue,
    double incomingInfluenceMagnitude,
    boolean relayedAsIs,
    String relayOriginAgentId,
    boolean ignoredTrustAndWeight,
    List<String> contributingSourceAgentIds) {
}
