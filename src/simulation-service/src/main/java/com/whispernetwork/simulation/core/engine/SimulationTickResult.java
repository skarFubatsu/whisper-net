package com.whispernetwork.simulation.core.engine;

import java.util.List;

/**
 * Aggregate result for one simulation tick.
 */
public record SimulationTickResult(int tickNumber, List<AgentOpinionUpdate> updates) {
}
