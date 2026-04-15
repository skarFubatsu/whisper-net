package com.whispernetwork.simulation.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class CoreModelAtomicTest {

  @Test
  void shouldValidatePersonaRangeBounds() {
    assertThrows(IllegalArgumentException.class, () -> new Persona(-0.01, 0.1, 0.1, 0.1));
    assertThrows(IllegalArgumentException.class, () -> new Persona(0.1, 1.01, 0.1, 0.1));
    assertThrows(IllegalArgumentException.class, () -> new Persona(0.1, 0.1, -0.01, 0.1));
    assertThrows(IllegalArgumentException.class, () -> new Persona(0.1, 0.1, 0.1, 1.01));
  }

  @Test
  void shouldDefaultNicknameAndRelayOriginForTrigger() {
    String id = UUID.randomUUID().toString();
    AgentState trigger = new AgentState(id, null, new Persona(0.1, 0.1, 0.9, 0.0), AgentRole.TRIGGER, 0.2);

    assertEquals(id, trigger.getNickname());
    assertEquals(id, trigger.getRelayOriginAgentId());
  }

  @Test
  void shouldRejectInvalidAgentStateValues() {
    assertThrows(IllegalArgumentException.class, () -> new AgentState(
        "bad-uuid",
        new Persona(0.1, 0.1, 0.9, 0.0),
        AgentRole.NORMAL,
        0.1));

    String id = UUID.randomUUID().toString();
    AgentState agent = new AgentState(id, new Persona(0.1, 0.1, 0.9, 0.0), AgentRole.NORMAL, 0.0);
    assertThrows(IllegalArgumentException.class, () -> agent.setOpinionValue(1.1));
    assertThrows(IllegalArgumentException.class, () -> agent.setRelayOriginAgentId("bad-uuid"));
  }

  @Test
  void shouldRejectInvalidRelationshipValues() {
    String a = UUID.randomUUID().toString();
    String b = UUID.randomUUID().toString();

    assertThrows(IllegalArgumentException.class, () -> new Relationship(
        "r1",
        "r2",
        a,
        b,
        -0.1,
        0.5,
        RelationshipType.FRIEND,
        RelationshipTransmissionMode.NORMAL_FLOW));

    assertThrows(IllegalArgumentException.class, () -> new Relationship(
        "r1",
        "r2",
        a,
        b,
        0.1,
        1.1,
        RelationshipType.FRIEND,
        RelationshipTransmissionMode.NORMAL_FLOW));
  }

  @Test
  void shouldEnforceNetworkTopologyConstraints() {
    InfluenceNetwork network = new InfluenceNetwork("network-1", 1);
    String a = UUID.randomUUID().toString();
    String b = UUID.randomUUID().toString();

    AgentState agentA = new AgentState(a, new Persona(0.1, 0.1, 0.9, 0.0), AgentRole.NORMAL, 0.1);
    AgentState agentB = new AgentState(b, new Persona(0.1, 0.1, 0.9, 0.0), AgentRole.NORMAL, -0.1);
    network.addAgent(agentA);
    network.addAgent(agentB);

    Relationship rel = new Relationship(
        "r1",
        "r2",
        a,
        b,
        0.8,
        0.7,
        RelationshipType.PEER,
        RelationshipTransmissionMode.NORMAL_FLOW);
    network.addRelationship(rel);

    assertEquals(1, network.getInboundRelationships(b).size());
    assertEquals(rel, network.getRelationship("r1"));

    assertThrows(IllegalArgumentException.class, () -> network.addRelationship(new Relationship(
        "r1",
        "r3",
        a,
        b,
        0.5,
        0.5,
        RelationshipType.NEUTRAL,
        RelationshipTransmissionMode.NORMAL_FLOW)));

    assertThrows(IllegalArgumentException.class, () -> network.getAgent(UUID.randomUUID().toString()));
    assertThrows(IllegalStateException.class, network::validateSingleTriggerPolicy);
  }
}
