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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SimulationTickEngine")
class SimulationTickEngineScenarioTest {

    private static final String CASCADE_TRIGGER_ID = "00000000-0000-0000-0000-000000000301";
    private static final String CASCADE_A_ID = "00000000-0000-0000-0000-000000000302";
    private static final String CASCADE_B_ID = "00000000-0000-0000-0000-000000000303";
    private static final String CASCADE_C_ID = "00000000-0000-0000-0000-000000000304";

    @Nested
    @DisplayName("Determinism")
    class DeterminismTests {

        @Test
        void shouldProcessQueueDeterministicallyWhenInfluenceMagnitudeTies() {
            String a = UUID.randomUUID().toString();
            String b = UUID.randomUUID().toString();
            String c = UUID.randomUUID().toString();

            InfluenceNetwork network = new InfluenceNetwork("deterministic-tie", 1);
            network.addAgent(new AgentState(a, new Persona(0.0, 0.0, 1.0, 0.0), AgentRole.TRIGGER, 0.9));
            network.addAgent(new AgentState(b, new Persona(0.0, 0.0, 1.0, 0.0), AgentRole.NORMAL, 0.0));
            network.addAgent(new AgentState(c, new Persona(0.0, 0.0, 1.0, 0.0), AgentRole.NORMAL, 0.0));

            addMirroredEdge(network, "r1", "r2", a, b, 1.0, 1.0, RelationshipTransmissionMode.NORMAL_FLOW);
            addMirroredEdge(network, "r3", "r4", a, c, 1.0, 1.0, RelationshipTransmissionMode.NORMAL_FLOW);

            SimulationTickEngine engine = new SimulationTickEngine(new OpinionAggregator());
            SimulationTickResult result = engine.executeTick(network, 1);

            List<String> updateOrder =
                    result.updates().stream().map(AgentOpinionUpdate::agentId).toList();

            // Trigger receives zero inbound influence here; B and C tie and are processed first by lexical id.
            List<String> expected = List.of(minLex(b, c), maxLex(b, c), a);
            assertEquals(expected, updateOrder);
        }
    }

    @Nested
    @DisplayName("Relay")
    class RelayTests {

        @Test
        void shouldRelayTriggerOpinionAsIsViaRelayChannel() {
            String trigger = UUID.randomUUID().toString();
            String relay = UUID.randomUUID().toString();
            String target = UUID.randomUUID().toString();

            InfluenceNetwork network = new InfluenceNetwork("relay-behavior", 1);
            AgentState triggerAgent = new AgentState(trigger, new Persona(0.0, 0.0, 1.0, 0.0), AgentRole.TRIGGER, 0.8);
            AgentState relayAgent = new AgentState(relay, new Persona(0.0, 0.0, 1.0, 0.0), AgentRole.RELAY, 0.8);
            relayAgent.setRelayOriginAgentId(trigger);
            AgentState targetAgent = new AgentState(target, new Persona(0.0, 0.0, 1.0, 0.0), AgentRole.NORMAL, -0.1);

            network.addAgent(triggerAgent);
            network.addAgent(relayAgent);
            network.addAgent(targetAgent);

            addMirroredEdge(network, "rt", "tr", trigger, relay, 1.0, 1.0, RelationshipTransmissionMode.NORMAL_FLOW);
            addMirroredEdge(network, "rx", "xr", relay, target, 0.01, 0.01, RelationshipTransmissionMode.RELAY_CHANNEL);

            SimulationTickResult result = new SimulationTickEngine(new OpinionAggregator()).executeTick(network, 1);
            AgentOpinionUpdate targetUpdate = result.updates().stream()
                    .filter(update -> update.agentId().equals(target))
                    .findFirst()
                    .orElseThrow();

            assertTrue(targetUpdate.relayedAsIs());
            assertTrue(targetUpdate.ignoredTrustAndWeight());
            assertEquals(trigger, targetUpdate.relayOriginAgentId());
            assertEquals(0.8, targetUpdate.newOpinionValue(), 0.00001);
        }
    }

    @Nested
    @DisplayName("Replay")
    class ReplayTests {

        @Test
        void shouldReplaySameScenarioDeterministicallyAcrossRuns() {
            SimulationTickEngine engine = new SimulationTickEngine(new OpinionAggregator());

            List<Map<String, Double>> firstRun = executeAndCapture(engine, createCascadeScenario(), 5);
            List<Map<String, Double>> secondRun = executeAndCapture(engine, createCascadeScenario(), 5);

            assertEquals(firstRun, secondRun);
        }
    }

    private static List<Map<String, Double>> executeAndCapture(
            SimulationTickEngine engine, InfluenceNetwork network, int ticks) {
        List<Map<String, Double>> snapshots = new ArrayList<>();
        for (int tick = 1; tick <= ticks; tick++) {
            engine.executeTick(network, tick);
            snapshots.add(snapshotOpinions(network));
        }
        return snapshots;
    }

    private static InfluenceNetwork createCascadeScenario() {
        InfluenceNetwork network = new InfluenceNetwork("cascade", 1);
        network.addAgent(new AgentState(CASCADE_TRIGGER_ID, new Persona(0.0, 0.0, 1.0, 0.0), AgentRole.TRIGGER, 0.8));
        network.addAgent(new AgentState(CASCADE_A_ID, new Persona(0.05, 0.2, 0.9, 0.0), AgentRole.NORMAL, 0.0));
        network.addAgent(new AgentState(CASCADE_B_ID, new Persona(0.05, 0.2, 0.9, 0.0), AgentRole.NORMAL, 0.0));
        network.addAgent(new AgentState(CASCADE_C_ID, new Persona(0.05, 0.2, 0.9, 0.0), AgentRole.NORMAL, 0.0));

        addMirroredEdge(
                network,
                "e1",
                "e2",
                CASCADE_TRIGGER_ID,
                CASCADE_A_ID,
                1.0,
                1.0,
                RelationshipTransmissionMode.NORMAL_FLOW);
        addMirroredEdge(
                network, "e3", "e4", CASCADE_A_ID, CASCADE_B_ID, 0.8, 0.9, RelationshipTransmissionMode.NORMAL_FLOW);
        addMirroredEdge(
                network, "e5", "e6", CASCADE_B_ID, CASCADE_C_ID, 0.7, 0.8, RelationshipTransmissionMode.NORMAL_FLOW);
        return network;
    }

    private static Map<String, Double> snapshotOpinions(InfluenceNetwork network) {
        return network.getAgents().stream()
                .sorted(Comparator.comparing(AgentState::getId))
                .collect(LinkedHashMap::new, (map, a) -> map.put(a.getId(), a.getOpinionValue()), Map::putAll);
    }

    private static void addMirroredEdge(
            InfluenceNetwork network,
            String forwardId,
            String reverseId,
            String source,
            String target,
            double weight,
            double trust,
            RelationshipTransmissionMode mode) {
        network.addRelationship(
                new Relationship(forwardId, reverseId, source, target, weight, trust, RelationshipType.FRIEND, mode));
        network.addRelationship(
                new Relationship(reverseId, forwardId, target, source, weight, trust, RelationshipType.FRIEND, mode));
    }

    private static String minLex(String a, String b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    private static String maxLex(String a, String b) {
        return a.compareTo(b) >= 0 ? a : b;
    }
}
