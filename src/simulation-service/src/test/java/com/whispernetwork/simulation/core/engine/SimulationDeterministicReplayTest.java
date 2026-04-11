package com.whispernetwork.simulation.core.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.whispernetwork.simulation.core.fixture.GoldenScenarioFixtures;
import com.whispernetwork.simulation.core.model.AgentState;
import com.whispernetwork.simulation.core.model.InfluenceNetwork;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SimulationDeterministicReplayTest {

  @Test
  void shouldReplayRelayScenarioDeterministicallyAcrossRuns() {
    List<Map<String, Double>> firstRun = executeAndCaptureTicks(
        GoldenScenarioFixtures.createRelayInjectionScenario(), 8);
    List<Map<String, Double>> secondRun = executeAndCaptureTicks(
        GoldenScenarioFixtures.createRelayInjectionScenario(), 8);

    assertEquals(firstRun, secondRun);
  }

  @Test
  void shouldReplayCascadeScenarioDeterministicallyAcrossRuns() {
    List<Map<String, Double>> firstRun = executeAndCaptureTicks(
        GoldenScenarioFixtures.createWeightedCascadeScenario(), 8);
    List<Map<String, Double>> secondRun = executeAndCaptureTicks(
        GoldenScenarioFixtures.createWeightedCascadeScenario(), 8);

    assertEquals(firstRun, secondRun);
  }

  private static List<Map<String, Double>> executeAndCaptureTicks(InfluenceNetwork network, int ticks) {
    SimulationTickEngine engine = new SimulationTickEngine(new OpinionAggregator());
    List<Map<String, Double>> snapshots = new ArrayList<>();

    for (int tick = 1; tick <= ticks; tick++) {
      engine.executeTick(network, tick);
      snapshots.add(snapshotOpinions(network));
    }
    return snapshots;
  }

  private static Map<String, Double> snapshotOpinions(InfluenceNetwork network) {
    return network.getAgents().stream()
        .sorted(Comparator.comparing(AgentState::getId))
        .collect(LinkedHashMap::new, (map, agent) -> map.put(agent.getId(), agent.getOpinionValue()), Map::putAll);
  }
}
