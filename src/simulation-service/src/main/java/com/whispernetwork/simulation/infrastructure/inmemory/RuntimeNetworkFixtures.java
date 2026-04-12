package com.whispernetwork.simulation.infrastructure.inmemory;

import com.whispernetwork.simulation.core.model.AgentRole;
import com.whispernetwork.simulation.core.model.AgentState;
import com.whispernetwork.simulation.core.model.InfluenceNetwork;
import com.whispernetwork.simulation.core.model.Persona;
import com.whispernetwork.simulation.core.model.Relationship;
import com.whispernetwork.simulation.core.model.RelationshipTransmissionMode;
import com.whispernetwork.simulation.core.model.RelationshipType;

/**
 * Runtime bootstrap fixture registration for local and container startup.
 */
public final class RuntimeNetworkFixtures {

  private RuntimeNetworkFixtures() {
  }

  /**
   * Registers built-in snapshots available at startup.
   */
  public static void registerDefaults(InMemoryInfluenceNetworkProvider provider) {
    provider.register(createWeightedCascadeScenario());
  }

  private static InfluenceNetwork createWeightedCascadeScenario() {
    InfluenceNetwork network = new InfluenceNetwork("golden-cascade", 1);

    String trigger = "00000000-0000-0000-0000-000000000301";
    String a = "00000000-0000-0000-0000-000000000302";
    String b = "00000000-0000-0000-0000-000000000303";
    String c = "00000000-0000-0000-0000-000000000304";

    network.addAgent(new AgentState(trigger, "T", new Persona(0.0, 0.0, 1.0, 0.0), AgentRole.TRIGGER, 0.8));
    network.addAgent(new AgentState(a, "A", new Persona(0.05, 0.2, 0.9, 0.0), AgentRole.NORMAL, 0.0));
    network.addAgent(new AgentState(b, "B", new Persona(0.05, 0.2, 0.9, 0.0), AgentRole.NORMAL, 0.0));
    network.addAgent(new AgentState(c, "C", new Persona(0.05, 0.2, 0.9, 0.0), AgentRole.NORMAL, 0.0));

    addMirroredEdge(network, "E11", "E12", trigger, a, 1.0, 1.0,
        RelationshipType.FRIEND, RelationshipTransmissionMode.NORMAL_FLOW);
    addMirroredEdge(network, "E13", "E14", a, b, 0.8, 0.9,
        RelationshipType.FRIEND, RelationshipTransmissionMode.NORMAL_FLOW);
    addMirroredEdge(network, "E15", "E16", b, c, 0.7, 0.8,
        RelationshipType.PEER, RelationshipTransmissionMode.NORMAL_FLOW);

    return network;
  }

  private static void addMirroredEdge(
      InfluenceNetwork network,
      String forwardId,
      String reverseId,
      String source,
      String target,
      double weight,
      double trust,
      RelationshipType type,
      RelationshipTransmissionMode mode) {
    network.addRelationship(new Relationship(forwardId, reverseId, source, target, weight, trust, type, mode));
    network.addRelationship(new Relationship(reverseId, forwardId, target, source, weight, trust, type, mode));
  }
}
