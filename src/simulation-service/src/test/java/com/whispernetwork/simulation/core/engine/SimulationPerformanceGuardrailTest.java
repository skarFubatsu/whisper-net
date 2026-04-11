package com.whispernetwork.simulation.core.engine;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.whispernetwork.simulation.core.fixture.GoldenScenarioFixtures;
import com.whispernetwork.simulation.core.model.InfluenceNetwork;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class SimulationPerformanceGuardrailTest {

  @Test
  void shouldKeepTickTimeWithinGuardrailsForTypicalSizes() {
    Assumptions.assumeTrue(Boolean.getBoolean("runPerformanceTests"),
        "Set -DrunPerformanceTests=true to run performance guardrails");

    long avg20 = averageTickMillis(GoldenScenarioFixtures.createScalableScenario(20), 8, 3);
    long avg100 = averageTickMillis(GoldenScenarioFixtures.createScalableScenario(100), 8, 3);
    long avg1000 = averageTickMillis(GoldenScenarioFixtures.createScalableScenario(1000), 5, 2);

    // Absolute ceilings are intentionally generous to avoid machine-specific flakes.
    assertTrue(avg20 <= 200, "20-node average tick should stay under 200ms, actual=" + avg20 + "ms");
    assertTrue(avg100 <= 700, "100-node average tick should stay under 700ms, actual=" + avg100 + "ms");
    assertTrue(avg1000 <= 6000, "1000-node average tick should stay under 6000ms, actual=" + avg1000 + "ms");
  }

  @Test
  void shouldScaleReasonablyBetweenSizes() {
    Assumptions.assumeTrue(Boolean.getBoolean("runPerformanceTests"),
        "Set -DrunPerformanceTests=true to run performance guardrails");

    long avg20 = averageTickMillis(GoldenScenarioFixtures.createScalableScenario(20), 8, 3);
    long avg100 = averageTickMillis(GoldenScenarioFixtures.createScalableScenario(100), 8, 3);
    long avg1000 = averageTickMillis(GoldenScenarioFixtures.createScalableScenario(1000), 5, 2);

    // Relative guardrails catch accidental complexity spikes while being hardware-tolerant.
    assertTrue(avg100 <= Math.max(1, avg20) * 20,
        "100-node tick should not exceed 20x 20-node tick. avg20=" + avg20 + "ms, avg100=" + avg100 + "ms");
    assertTrue(avg1000 <= Math.max(1, avg100) * 20,
        "1000-node tick should not exceed 20x 100-node tick. avg100=" + avg100 + "ms, avg1000=" + avg1000 + "ms");
  }

  private static long averageTickMillis(InfluenceNetwork network, int measuredTicks, int warmupTicks) {
    SimulationTickEngine engine = new SimulationTickEngine(new OpinionAggregator());

    int tick = 1;
    for (int i = 0; i < warmupTicks; i++, tick++) {
      engine.executeTick(network, tick);
    }

    long totalNanos = 0L;
    for (int i = 0; i < measuredTicks; i++, tick++) {
      long start = System.nanoTime();
      engine.executeTick(network, tick);
      totalNanos += System.nanoTime() - start;
    }

    return TimeUnit.NANOSECONDS.toMillis(totalNanos / Math.max(1, measuredTicks));
  }
}
