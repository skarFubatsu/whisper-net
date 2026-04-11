package com.whispernetwork.simulation.core.engine;

import com.whispernetwork.simulation.core.model.AgentState;
import com.whispernetwork.simulation.core.model.InfluenceNetwork;
import com.whispernetwork.simulation.core.model.Relationship;

/**
 * Validates core invariants for simulation inputs and network consistency.
 */
public final class SimulationInvariantChecker {

  /**
   * Validates network-level invariants.
   *
   * @param network network state
   */
  public void validateNetwork(InfluenceNetwork network) {
    network.validateSingleTriggerPolicy();

    for (AgentState agent : network.getAgents()) {
      if (agent.getOpinionValue() < -1.0 || agent.getOpinionValue() > 1.0) {
        throw new IllegalStateException("Agent opinion out of bounds for agent " + agent.getId());
      }
    }

    for (Relationship relationship : network.getRelationships()) {
      // WHY: mirror integrity is required so update/delete semantics remain deterministic and symmetric.
      validateMirroredRelationship(network, relationship);
      network.getAgent(relationship.sourceAgentId());
      network.getAgent(relationship.targetAgentId());
    }
  }

  /**
   * Validates that a relationship has a correct reverse partner.
   *
   * @param network network state
   * @param relationship relationship to validate
   */
  private void validateMirroredRelationship(InfluenceNetwork network, Relationship relationship) {
    if (relationship.reverseRelationshipId() == null || relationship.reverseRelationshipId().isBlank()) {
      throw new IllegalStateException("Missing reverse relationship id for " + relationship.id());
    }

    Relationship reverse = network.getRelationship(relationship.reverseRelationshipId());
    if (reverse == null) {
      throw new IllegalStateException("Reverse relationship not found for " + relationship.id());
    }

    if (!relationship.id().equals(reverse.reverseRelationshipId())) {
      throw new IllegalStateException("Reverse link mismatch between " + relationship.id() + " and " + reverse.id());
    }

    if (!relationship.sourceAgentId().equals(reverse.targetAgentId())
        || !relationship.targetAgentId().equals(reverse.sourceAgentId())) {
      throw new IllegalStateException("Reverse direction mismatch for relationship " + relationship.id());
    }

    if (Double.compare(relationship.weight(), reverse.weight()) != 0
        || Double.compare(relationship.trustValue(), reverse.trustValue()) != 0
        || relationship.relationshipType() != reverse.relationshipType()
        || relationship.transmissionMode() != reverse.transmissionMode()) {
      throw new IllegalStateException("Mirror attributes mismatch for relationship " + relationship.id());
    }
  }
}
