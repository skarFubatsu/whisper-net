package com.whispernetwork.simulation.core.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.whispernetwork.simulation.core.model.AgentRole;
import com.whispernetwork.simulation.core.model.AgentState;
import com.whispernetwork.simulation.core.model.InfluenceNetwork;
import com.whispernetwork.simulation.core.model.Persona;
import com.whispernetwork.simulation.core.model.Relationship;
import com.whispernetwork.simulation.core.model.RelationshipTransmissionMode;
import com.whispernetwork.simulation.core.model.RelationshipType;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CoreEngineAtomic")
class CoreEngineAtomicTest {

    @Nested
    @DisplayName("Aggregation")
    class AggregationTests {

        @Test
        void shouldAggregateWithWeightedNormalFlow() {
            InfluenceNetwork network = networkWithSingleTriggerAndNormalTarget();
            AgentState target = network.getAgent(targetId(network));

            OpinionAggregationResult result = new OpinionAggregator().aggregate(network, target);

            assertFalse(result.relayedAsIs());
            assertFalse(result.ignoredTrustAndWeight());
            assertTrue(result.incomingInfluenceMagnitude() > 0.0);
            assertTrue(result.newOpinionValue() >= -1.0 && result.newOpinionValue() <= 1.0);
            assertEquals(1, result.contributingSourceAgentIds().size());
        }
    }

    @Nested
    @DisplayName("Relay")
    class RelayTests {

        @Test
        void shouldRelayAsIsForRelayChannelInput() {
            String trigger = UUID.randomUUID().toString();
            String relay = UUID.randomUUID().toString();
            String target = UUID.randomUUID().toString();

            InfluenceNetwork network = new InfluenceNetwork("relay-net", 1);
            AgentState triggerAgent = new AgentState(trigger, new Persona(0.0, 0.1, 1.0, 0.0), AgentRole.TRIGGER, 0.9);
            AgentState relayAgent = new AgentState(relay, new Persona(0.0, 0.1, 1.0, 0.0), AgentRole.RELAY, 0.9);
            relayAgent.setRelayOriginAgentId(trigger);
            AgentState targetAgent = new AgentState(target, new Persona(0.0, 0.2, 0.9, 0.0), AgentRole.NORMAL, -0.2);

            network.addAgent(triggerAgent);
            network.addAgent(relayAgent);
            network.addAgent(targetAgent);

            addMirror(network, "tr", "rt", trigger, relay, 1.0, 1.0, RelationshipTransmissionMode.NORMAL_FLOW);
            addMirror(network, "rx", "xr", relay, target, 0.01, 0.01, RelationshipTransmissionMode.RELAY_CHANNEL);

            OpinionAggregationResult result = new OpinionAggregator().aggregate(network, targetAgent);

            assertTrue(result.relayedAsIs());
            assertTrue(result.ignoredTrustAndWeight());
            assertEquals(trigger, result.relayOriginAgentId());
            assertEquals(0.9, result.newOpinionValue(), 0.00001);
        }
    }

    @Nested
    @DisplayName("Validation")
    class ValidationTests {

        @Test
        void shouldValidateMirrorConsistencyAndRejectBrokenNetwork() {
            InfluenceNetwork valid = networkWithSingleTriggerAndNormalTarget();
            SimulationInvariantChecker checker = new SimulationInvariantChecker();
            checker.validateNetwork(valid);

            InfluenceNetwork broken = networkWithSingleTriggerAndNormalTarget();
            String source = triggerId(broken);
            String target = targetId(broken);
            broken.addRelationship(new Relationship(
                    "broken-forward",
                    "missing-reverse",
                    source,
                    target,
                    1.0,
                    1.0,
                    RelationshipType.FRIEND,
                    RelationshipTransmissionMode.NORMAL_FLOW));

            assertThrows(IllegalStateException.class, () -> checker.validateNetwork(broken));
        }
    }

    private static InfluenceNetwork networkWithSingleTriggerAndNormalTarget() {
        String trigger = UUID.randomUUID().toString();
        String target = UUID.randomUUID().toString();

        InfluenceNetwork network = new InfluenceNetwork("net-1", 1);
        network.addAgent(new AgentState(trigger, new Persona(0.0, 0.1, 1.0, 0.0), AgentRole.TRIGGER, 0.8));
        network.addAgent(new AgentState(target, new Persona(0.0, 0.2, 0.9, 0.0), AgentRole.NORMAL, 0.0));
        addMirror(network, "fwd", "rev", trigger, target, 1.0, 1.0, RelationshipTransmissionMode.NORMAL_FLOW);
        return network;
    }

    private static void addMirror(
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

    private static String triggerId(InfluenceNetwork network) {
        return network.getAgents().stream()
                .filter(agent -> agent.getRole() == AgentRole.TRIGGER)
                .findFirst()
                .orElseThrow()
                .getId();
    }

    private static String targetId(InfluenceNetwork network) {
        return network.getAgents().stream()
                .filter(agent -> agent.getRole() == AgentRole.NORMAL)
                .findFirst()
                .orElseThrow()
                .getId();
    }
}
