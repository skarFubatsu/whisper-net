package com.whispernetwork.simulation.core.engine;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.whispernetwork.simulation.core.fixture.GoldenScenarioFixtures;
import com.whispernetwork.simulation.core.model.InfluenceNetwork;
import org.junit.jupiter.api.Test;

class SimulationInvariantCheckerTest {

  @Test
  void shouldPassForValidMirroredFixture() {
    InfluenceNetwork network = GoldenScenarioFixtures.createWeightedCascadeScenario();

    SimulationInvariantChecker checker = new SimulationInvariantChecker();
    assertDoesNotThrow(() -> checker.validateNetwork(network));
  }

  @Test
  void shouldFailWhenReverseRelationshipIsMissing() {
    InfluenceNetwork network = GoldenScenarioFixtures.createWeightedCascadeScenario();
    network.addRelationship(new com.whispernetwork.simulation.core.model.Relationship(
        "BROKEN", "MISSING", "00000000-0000-0000-0000-000000000301", "00000000-0000-0000-0000-000000000304",
        1.0, 1.0,
        com.whispernetwork.simulation.core.model.RelationshipType.FRIEND,
        com.whispernetwork.simulation.core.model.RelationshipTransmissionMode.NORMAL_FLOW));

    SimulationInvariantChecker checker = new SimulationInvariantChecker();
    assertThrows(IllegalStateException.class, () -> checker.validateNetwork(network));
  }
}
