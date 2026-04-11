package com.whispernetwork.simulation.core.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory network snapshot used for simulation.
 */
public final class InfluenceNetwork {
  private final String id;
  private final int versionNumber;
  private final Map<String, AgentState> agents;
  private final Map<String, Relationship> relationshipsById;
  private final Map<String, List<Relationship>> inboundRelationships;

  /**
   * Creates an empty network.
   *
   * @param id network id
   * @param versionNumber version number, starting at 1
   */
  public InfluenceNetwork(String id, int versionNumber) {
    if (versionNumber < 1) {
      throw new IllegalArgumentException("versionNumber must be >= 1");
    }
    this.id = id;
    this.versionNumber = versionNumber;
    this.agents = new HashMap<>();
    this.relationshipsById = new HashMap<>();
    this.inboundRelationships = new HashMap<>();
  }

  /**
   * Returns network id.
   */
  public String getId() {
    return id;
  }

  /**
   * Returns version number.
   */
  public int getVersionNumber() {
    return versionNumber;
  }

  /**
   * Adds an agent to the network.
   *
   * @param agent agent state
   */
  public void addAgent(AgentState agent) {
    agents.put(agent.getId(), agent);
    inboundRelationships.putIfAbsent(agent.getId(), new ArrayList<>());
  }

  /**
   * Adds a directed relationship.
   *
   * @param relationship relationship edge
   */
  public void addRelationship(Relationship relationship) {
    if (relationshipsById.containsKey(relationship.id())) {
      throw new IllegalArgumentException("Relationship id already exists: " + relationship.id());
    }
    if (!agents.containsKey(relationship.sourceAgentId()) || !agents.containsKey(relationship.targetAgentId())) {
      throw new IllegalArgumentException("Both source and target agents must exist before adding a relationship");
    }
    relationshipsById.put(relationship.id(), relationship);
    inboundRelationships.computeIfAbsent(relationship.targetAgentId(), ignored -> new ArrayList<>()).add(relationship);
  }

  /**
   * Returns all agent states.
   */
  public Collection<AgentState> getAgents() {
    return Collections.unmodifiableCollection(agents.values());
  }

  /**
   * Returns all relationships in the network.
   */
  public Collection<Relationship> getRelationships() {
    return Collections.unmodifiableCollection(relationshipsById.values());
  }

  /**
   * Returns a relationship by id.
   *
   * @param relationshipId relationship id
   * @return relationship or null when not found
   */
  public Relationship getRelationship(String relationshipId) {
    return relationshipsById.get(relationshipId);
  }

  /**
   * Returns inbound relationships for a target agent.
   *
   * @param targetAgentId target agent id
   * @return inbound relationship list
   */
  public List<Relationship> getInboundRelationships(String targetAgentId) {
    return inboundRelationships.getOrDefault(targetAgentId, List.of());
  }

  /**
   * Returns an agent by id.
   *
   * @param agentId agent id
   * @return agent state
   */
  public AgentState getAgent(String agentId) {
    AgentState agent = agents.get(agentId);
    if (agent == null) {
      throw new IllegalArgumentException("Unknown agent id: " + agentId);
    }
    return agent;
  }

  /**
   * Validates trigger policy.
   */
  public void validateSingleTriggerPolicy() {
    long triggerCount = agents.values().stream().filter(a -> a.getRole() == AgentRole.TRIGGER).count();
    if (triggerCount != 1) {
      throw new IllegalStateException("MVP requires exactly one trigger agent");
    }
  }
}
