package com.whispernetwork.simulation.core.model;

import java.util.UUID;

/**
 * Mutable agent state used by the tick engine.
 */
public final class AgentState {
  private final String id;
  private final String nickname;
  private final Persona persona;
  private final AgentRole role;
  private double opinionValue;
  private String relayOriginAgentId;

  /**
   * Creates agent state.
   *
   * @param id agent id (UUID string)
   * @param persona agent persona
   * @param role role in network
   * @param opinionValue current opinion in [-1, 1]
   */
  public AgentState(String id, Persona persona, AgentRole role, double opinionValue) {
    this(id, null, persona, role, opinionValue);
  }

  /**
   * Creates agent state.
   *
   * @param id agent id (UUID string)
   * @param nickname optional nickname; defaults to id when null/blank
   * @param persona agent persona
   * @param role role in network
   * @param opinionValue current opinion in [-1, 1]
   */
  public AgentState(String id, String nickname, Persona persona, AgentRole role, double opinionValue) {
    validateUuid("id", id);
    this.id = id;
    this.nickname = nickname == null || nickname.isBlank() ? id : nickname;
    this.persona = persona;
    this.role = role;
    setOpinionValue(opinionValue);
    this.relayOriginAgentId = role == AgentRole.TRIGGER ? id : null;
  }

  /**
   * Returns the agent id.
   */
  public String getId() {
    return id;
  }

  /**
   * Returns the UI-friendly nickname.
   */
  public String getNickname() {
    return nickname;
  }

  /**
   * Returns the persona.
   */
  public Persona getPersona() {
    return persona;
  }

  /**
   * Returns the role.
   */
  public AgentRole getRole() {
    return role;
  }

  /**
   * Returns the current opinion.
   */
  public double getOpinionValue() {
    return opinionValue;
  }

  /**
   * Sets the current opinion.
   *
   * @param opinionValue opinion in [-1, 1]
   */
  public void setOpinionValue(double opinionValue) {
    if (opinionValue < -1.0 || opinionValue > 1.0) {
      throw new IllegalArgumentException("opinionValue must be in [-1,1]");
    }
    this.opinionValue = opinionValue;
  }

  /**
   * Returns relay origin agent id, if present.
   */
  public String getRelayOriginAgentId() {
    return relayOriginAgentId;
  }

  /**
   * Sets relay origin id.
   *
   * @param relayOriginAgentId relay origin id
   */
  public void setRelayOriginAgentId(String relayOriginAgentId) {
    if (relayOriginAgentId != null) {
      validateUuid("relayOriginAgentId", relayOriginAgentId);
    }
    this.relayOriginAgentId = relayOriginAgentId;
  }

  /**
   * Validates that a string is a UUID.
   *
   * @param fieldName field name
   * @param value string value
   */
  private static void validateUuid(String fieldName, String value) {
    try {
      UUID.fromString(value);
    } catch (RuntimeException ex) {
      throw new IllegalArgumentException(fieldName + " must be a valid UUID", ex);
    }
  }
}
