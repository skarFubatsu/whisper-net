package com.whispernetwork.simulation.core.engine;

import com.whispernetwork.simulation.core.model.AgentState;
import com.whispernetwork.simulation.core.model.InfluenceNetwork;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Executes one deterministic cascading simulation tick.
 */
public final class SimulationTickEngine {
  private final OpinionAggregator opinionAggregator;
  private final SimulationInvariantChecker invariantChecker;

  /**
   * Creates a tick engine.
   *
   * @param opinionAggregator opinion aggregator
   */
  public SimulationTickEngine(OpinionAggregator opinionAggregator) {
    this.opinionAggregator = opinionAggregator;
    this.invariantChecker = new SimulationInvariantChecker();
  }

  /**
   * Executes one tick using cascading updates and deterministic priority ordering.
   *
   * @param network network state
   * @param tickNumber current tick number
   * @return tick result
   */
  public SimulationTickResult executeTick(InfluenceNetwork network, int tickNumber) {
    invariantChecker.validateNetwork(network);

    PriorityQueue<PendingAgent> queue = new PriorityQueue<>(
        Comparator.<PendingAgent>comparingDouble(p -> p.incomingMagnitude).reversed()
            .thenComparing(p -> p.agentId));

    for (AgentState agent : network.getAgents()) {
      OpinionAggregationResult preview = opinionAggregator.aggregate(network, agent);
      queue.offer(new PendingAgent(agent.getId(), preview.incomingInfluenceMagnitude()));
    }

    List<AgentOpinionUpdate> updates = new ArrayList<>();

    while (!queue.isEmpty()) {
      PendingAgent pending = queue.poll();
      AgentState target = network.getAgent(pending.agentId);
      double previous = target.getOpinionValue();
      OpinionAggregationResult aggregated = opinionAggregator.aggregate(network, target);
      target.setOpinionValue(aggregated.newOpinionValue());

      if (aggregated.relayedAsIs() && aggregated.relayOriginAgentId() != null) {
        target.setRelayOriginAgentId(aggregated.relayOriginAgentId());
      }

      updates.add(new AgentOpinionUpdate(
          target.getId(),
          previous,
          aggregated.newOpinionValue(),
          aggregated.incomingInfluenceMagnitude(),
          aggregated.relayedAsIs(),
          aggregated.relayOriginAgentId(),
          aggregated.ignoredTrustAndWeight(),
          aggregated.contributingSourceAgentIds()));
    }

    return new SimulationTickResult(tickNumber, List.copyOf(updates));
  }

  private record PendingAgent(String agentId, double incomingMagnitude) {
  }
}
