package com.whispernetwork.simulation.core.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.whispernetwork.simulation.core.model.AgentRole;
import com.whispernetwork.simulation.core.model.AgentState;
import com.whispernetwork.simulation.core.model.InfluenceNetwork;
import com.whispernetwork.simulation.core.model.Persona;
import com.whispernetwork.simulation.core.model.Relationship;
import com.whispernetwork.simulation.core.model.RelationshipTransmissionMode;
import com.whispernetwork.simulation.core.model.RelationshipType;
import java.util.List;
import org.junit.jupiter.api.Test;

class SimulationTickEngineTest {

    private static final String AGENT_A_ID = "00000000-0000-0000-0000-00000000000a";
    private static final String AGENT_B_ID = "00000000-0000-0000-0000-00000000000b";
    private static final String AGENT_C_ID = "00000000-0000-0000-0000-00000000000c";

    private static final String TRIGGER_ID = "00000000-0000-0000-0000-000000000101";
    private static final String RELAY_ID = "00000000-0000-0000-0000-000000000102";
    private static final String TARGET_ID = "00000000-0000-0000-0000-000000000103";

  @Test
  void shouldProcessQueueDeterministicallyWhenMagnitudeTies() {
    InfluenceNetwork network = new InfluenceNetwork("network-1", 1);

    AgentState trigger = new AgentState(AGENT_A_ID, null, new Persona(0.0, 0.0, 1.0, 0.0), AgentRole.TRIGGER, 0.9);
    AgentState b = new AgentState(AGENT_B_ID, "", new Persona(0.0, 0.0, 1.0, 0.0), AgentRole.NORMAL, 0.0);
    AgentState c = new AgentState(AGENT_C_ID, "Agent C", new Persona(0.0, 0.0, 1.0, 0.0), AgentRole.NORMAL, 0.0);
    assertEquals(AGENT_B_ID, b.getNickname());
    assertEquals("Agent C", c.getNickname());

    network.addAgent(trigger);
    network.addAgent(b);
    network.addAgent(c);

    network.addRelationship(new Relationship("R1", "R2", AGENT_A_ID, AGENT_B_ID, 1.0, 1.0, RelationshipType.FRIEND,
        RelationshipTransmissionMode.NORMAL_FLOW));
    network.addRelationship(new Relationship("R2", "R1", AGENT_B_ID, AGENT_A_ID, 1.0, 1.0, RelationshipType.FRIEND,
        RelationshipTransmissionMode.NORMAL_FLOW));
    network.addRelationship(new Relationship("R3", "R4", AGENT_A_ID, AGENT_C_ID, 1.0, 1.0, RelationshipType.FRIEND,
        RelationshipTransmissionMode.NORMAL_FLOW));
    network.addRelationship(new Relationship("R4", "R3", AGENT_C_ID, AGENT_A_ID, 1.0, 1.0, RelationshipType.FRIEND,
        RelationshipTransmissionMode.NORMAL_FLOW));

    SimulationTickEngine engine = new SimulationTickEngine(new OpinionAggregator());
    SimulationTickResult result = engine.executeTick(network, 1);

    List<String> order = result.updates().stream().map(AgentOpinionUpdate::agentId).toList();
    assertEquals(List.of(AGENT_B_ID, AGENT_C_ID, AGENT_A_ID), order);
  }

  @Test
  void shouldRelayTriggerOpinionAsIsOverRelayChannelAndIgnoreTrustWeight() {
    InfluenceNetwork network = new InfluenceNetwork("network-2", 1);

    AgentState trigger = new AgentState(TRIGGER_ID, new Persona(0.0, 0.0, 1.0, 0.0), AgentRole.TRIGGER, 0.8);
    AgentState relay = new AgentState(RELAY_ID, new Persona(0.0, 0.0, 1.0, 0.0), AgentRole.RELAY, 0.8);
    relay.setRelayOriginAgentId(TRIGGER_ID);
    AgentState target = new AgentState(TARGET_ID, new Persona(0.0, 0.0, 1.0, 0.0), AgentRole.NORMAL, -0.1);

    network.addAgent(trigger);
    network.addAgent(relay);
    network.addAgent(target);

    network.addRelationship(new Relationship("RT", "TR", TRIGGER_ID, RELAY_ID, 1.0, 1.0, RelationshipType.PEER,
        RelationshipTransmissionMode.NORMAL_FLOW));
    network.addRelationship(new Relationship("TR", "RT", RELAY_ID, TRIGGER_ID, 1.0, 1.0, RelationshipType.PEER,
        RelationshipTransmissionMode.NORMAL_FLOW));

    network.addRelationship(new Relationship("RX", "XR", RELAY_ID, TARGET_ID, 0.01, 0.01, RelationshipType.NEUTRAL,
        RelationshipTransmissionMode.RELAY_CHANNEL));
    network.addRelationship(new Relationship("XR", "RX", TARGET_ID, RELAY_ID, 0.01, 0.01, RelationshipType.NEUTRAL,
        RelationshipTransmissionMode.RELAY_CHANNEL));

    SimulationTickEngine engine = new SimulationTickEngine(new OpinionAggregator());
    SimulationTickResult result = engine.executeTick(network, 1);

    AgentOpinionUpdate targetUpdate = result.updates().stream()
        .filter(update -> update.agentId().equals(TARGET_ID))
        .findFirst()
        .orElseThrow();

    assertTrue(targetUpdate.relayedAsIs());
    assertTrue(targetUpdate.ignoredTrustAndWeight());
        assertEquals(TRIGGER_ID, targetUpdate.relayOriginAgentId());
    assertEquals(0.8, targetUpdate.newOpinionValue(), 0.0001);
  }
}
