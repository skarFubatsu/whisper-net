package com.whispernetwork.simulation.core.fixture;

import com.whispernetwork.simulation.core.model.AgentRole;
import com.whispernetwork.simulation.core.model.AgentState;
import com.whispernetwork.simulation.core.model.InfluenceNetwork;
import com.whispernetwork.simulation.core.model.Persona;
import com.whispernetwork.simulation.core.model.Relationship;
import com.whispernetwork.simulation.core.model.RelationshipTransmissionMode;
import com.whispernetwork.simulation.core.model.RelationshipType;

/**
 * Shared golden fixtures for deterministic simulation tests.
 */
public final class GoldenScenarioFixtures {

  private GoldenScenarioFixtures() {
  }

  /**
   * Builds a relay injection scenario for deterministic replay tests.
   *
   * @return initialized influence network
   */
  public static InfluenceNetwork createRelayInjectionScenario() {
    InfluenceNetwork network = new InfluenceNetwork("golden-relay", 1);

    String trigger = "00000000-0000-0000-0000-000000000201";
    String relay = "00000000-0000-0000-0000-000000000202";
    String listenerOne = "00000000-0000-0000-0000-000000000203";
    String listenerTwo = "00000000-0000-0000-0000-000000000204";

    AgentState triggerAgent = new AgentState(trigger, "Trigger", new Persona(0.0, 0.1, 1.0, 0.0), AgentRole.TRIGGER, 0.9);
    AgentState relayAgent = new AgentState(relay, "Relay", new Persona(0.0, 0.1, 1.0, 0.0), AgentRole.RELAY, 0.9);
    relayAgent.setRelayOriginAgentId(trigger);

    AgentState listenerOneAgent = new AgentState(listenerOne, "Listener One",
        new Persona(0.1, 0.2, 0.8, 0.0), AgentRole.NORMAL, -0.2);
    AgentState listenerTwoAgent = new AgentState(listenerTwo, "Listener Two",
        new Persona(0.0, 0.3, 0.7, 0.0), AgentRole.NORMAL, -0.1);

    network.addAgent(triggerAgent);
    network.addAgent(relayAgent);
    network.addAgent(listenerOneAgent);
    network.addAgent(listenerTwoAgent);

    addMirroredEdge(network, "E1", "E2", trigger, relay, 1.0, 1.0,
        RelationshipType.PEER, RelationshipTransmissionMode.NORMAL_FLOW);
    addMirroredEdge(network, "E3", "E4", relay, listenerOne, 0.01, 0.01,
        RelationshipType.NEUTRAL, RelationshipTransmissionMode.RELAY_CHANNEL);
    addMirroredEdge(network, "E5", "E6", relay, listenerTwo, 0.01, 0.01,
        RelationshipType.NEUTRAL, RelationshipTransmissionMode.RELAY_CHANNEL);

    return network;
  }

  /**
   * Builds a weighted cascade scenario without relay channels.
   *
   * @return initialized influence network
   */
  public static InfluenceNetwork createWeightedCascadeScenario() {
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

    /**
     * Builds a deterministic scalable scenario for performance guardrails.
     *
     * @param nodeCount number of nodes, minimum 2
     * @return initialized influence network
     */
    public static InfluenceNetwork createScalableScenario(int nodeCount) {
        if (nodeCount < 2) {
            throw new IllegalArgumentException("nodeCount must be >= 2");
        }

        InfluenceNetwork network = new InfluenceNetwork("golden-scale-" + nodeCount, 1);

        String triggerId = deterministicUuid(1);
        network.addAgent(new AgentState(
                triggerId,
                "Trigger",
                new Persona(0.0, 0.1, 1.0, 0.0),
                AgentRole.TRIGGER,
                0.8));

        for (int index = 2; index <= nodeCount; index++) {
            String agentId = deterministicUuid(index);
            network.addAgent(new AgentState(
                    agentId,
                    "Agent-" + index,
                    new Persona(0.05, 0.15, 0.9, 0.0),
                    AgentRole.NORMAL,
                    0.0));
        }

        // WHY: this combination of star + ring creates dense-enough influence paths while
        // remaining deterministic and linear to construct across larger node counts.
        for (int index = 2; index <= nodeCount; index++) {
            String targetId = deterministicUuid(index);
            addMirroredEdge(
                    network,
                    "S-F-" + index,
                    "S-R-" + index,
                    triggerId,
                    targetId,
                    0.8,
                    0.9,
                    RelationshipType.FRIEND,
                    RelationshipTransmissionMode.NORMAL_FLOW);
        }

        for (int index = 2; index <= nodeCount; index++) {
            int next = index == nodeCount ? 2 : index + 1;
            String sourceId = deterministicUuid(index);
            String targetId = deterministicUuid(next);
            addMirroredEdge(
                    network,
                    "R-F-" + index,
                    "R-R-" + index,
                    sourceId,
                    targetId,
                    0.6,
                    0.7,
                    RelationshipType.PEER,
                    RelationshipTransmissionMode.NORMAL_FLOW);
        }

        return network;
    }

  /**
   * Adds a mirrored relationship pair to a network.
   */
  public static void addMirroredEdge(
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

    /**
     * Produces deterministic UUIDs for fixture generation.
     */
    private static String deterministicUuid(int index) {
        return String.format("00000000-0000-0000-0000-%012x", index);
    }
}
