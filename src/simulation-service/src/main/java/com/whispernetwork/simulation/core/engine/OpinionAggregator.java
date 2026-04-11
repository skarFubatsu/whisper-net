package com.whispernetwork.simulation.core.engine;

import com.whispernetwork.simulation.core.model.AgentRole;
import com.whispernetwork.simulation.core.model.AgentState;
import com.whispernetwork.simulation.core.model.InfluenceNetwork;
import com.whispernetwork.simulation.core.model.Relationship;
import com.whispernetwork.simulation.core.model.RelationshipTransmissionMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Computes next opinion values for a target agent.
 */
public final class OpinionAggregator {

  /**
   * Aggregates opinion for the target agent.
   *
   * @param network network state
   * @param targetAgent target agent
   * @return aggregation result
   */
  public OpinionAggregationResult aggregate(InfluenceNetwork network, AgentState targetAgent) {
    List<Relationship> inbound = network.getInboundRelationships(targetAgent.getId());
    if (inbound.isEmpty()) {
      return new OpinionAggregationResult(
          targetAgent.getOpinionValue(),
          0.0,
          false,
          null,
          false,
          List.of());
    }

    double weightedSum = 0.0;
    double relaySum = 0.0;
    int weightedCount = 0;
    int relayCount = 0;
    List<String> contributors = new ArrayList<>();
    String relayOrigin = null;

    for (Relationship relationship : inbound) {
      AgentState source = network.getAgent(relationship.sourceAgentId());
      contributors.add(source.getId());

      boolean relayPath = relationship.transmissionMode() == RelationshipTransmissionMode.RELAY_CHANNEL
          && source.getRole() == AgentRole.RELAY
          && source.getRelayOriginAgentId() != null;

      if (relayPath) {
        relaySum += source.getOpinionValue();
        relayCount++;
        if (relayOrigin == null) {
          relayOrigin = source.getRelayOriginAgentId();
        }
        continue;
      }

      weightedSum += source.getOpinionValue() * relationship.weight() * relationship.trustValue();
      weightedCount++;
    }

    double incomingInfluence;
    boolean relayedAsIs = relayCount > 0;
    boolean ignoredTrustAndWeight = relayedAsIs;

    if (relayCount > 0) {
      incomingInfluence = relaySum / relayCount;
    } else if (weightedCount > 0) {
      incomingInfluence = weightedSum / weightedCount;
    } else {
      incomingInfluence = 0.0;
    }

    if (relayedAsIs) {
      double relayedOpinion = clamp(incomingInfluence, -1.0, 1.0);
      return new OpinionAggregationResult(
          relayedOpinion,
          Math.abs(incomingInfluence),
          true,
          relayOrigin,
          true,
          List.copyOf(contributors));
    }

    double selfWeight = 1.0 + targetAgent.getPersona().stubbornness();
    double biasModifier = targetAgent.getPersona().bias() * sign(targetAgent.getOpinionValue());
    double newOpinion = selfWeight * targetAgent.getOpinionValue()
        + targetAgent.getPersona().susceptibility() * incomingInfluence
        + biasModifier;

    double clamped = clamp(newOpinion, -1.0, 1.0);

    return new OpinionAggregationResult(
        clamped,
        Math.abs(incomingInfluence),
        relayedAsIs,
        relayOrigin,
        ignoredTrustAndWeight,
        List.copyOf(contributors));
  }

  /**
   * Computes sign for non-zero values.
   *
   * @param value numeric value
   * @return -1, 0, or 1
   */
  public static double sign(double value) {
    if (value > 0.0) {
      return 1.0;
    }
    if (value < 0.0) {
      return -1.0;
    }
    return 0.0;
  }

  /**
   * Clamps a value to a closed interval.
   *
   * @param value value
   * @param min minimum
   * @param max maximum
   * @return clamped value
   */
  public static double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }
}
