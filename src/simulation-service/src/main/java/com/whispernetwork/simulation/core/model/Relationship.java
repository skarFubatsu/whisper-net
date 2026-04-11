package com.whispernetwork.simulation.core.model;

/**
 * Directed relationship edge from source to target.
 */
public record Relationship(
    String id,
    String reverseRelationshipId,
    String sourceAgentId,
    String targetAgentId,
    double weight,
    double trustValue,
    RelationshipType relationshipType,
    RelationshipTransmissionMode transmissionMode) {

  /**
   * Validates edge values.
   */
  public Relationship {
    if (trustValue < 0.0 || trustValue > 1.0) {
      throw new IllegalArgumentException("trustValue must be in [0,1]");
    }
    if (weight < 0.0) {
      throw new IllegalArgumentException("weight must be >= 0");
    }
  }
}
