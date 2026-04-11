package com.whispernetwork.simulation.application.model;

import com.whispernetwork.simulation.core.engine.AgentOpinionUpdate;
import java.time.Instant;
import java.util.List;

/**
 * Base event contract emitted by the simulation application service.
 */
public sealed interface SimulationEvent
    permits SimulationEvent.SimulationStarted,
    SimulationEvent.SimulationTickCompleted,
    SimulationEvent.SimulationCancelled,
    SimulationEvent.SimulationCompleted,
    SimulationEvent.SimulationFailed {

  /**
   * Returns run id associated with the event.
   */
  String simulationRunId();

  /**
   * Returns network id associated with the event.
   */
  String networkId();

  /**
   * Returns event timestamp.
   */
  Instant occurredAt();

  /**
   * Emitted when a run starts execution.
   */
    record SimulationStarted(
            String simulationRunId,
            String networkId,
            int networkVersionNumber,
            String initiatedByActorId,
            String clientRequestId,
            Instant occurredAt) implements SimulationEvent {
  }

  /**
   * Emitted for every completed tick.
   */
  record SimulationTickCompleted(
      String simulationRunId,
      String networkId,
      int tickNumber,
      int updatedAgents,
      List<AgentOpinionUpdate> updates,
      Instant occurredAt) implements SimulationEvent {
  }

  /**
   * Emitted when cancellation is completed.
   */
    record SimulationCancelled(
            String simulationRunId,
            String networkId,
            String cancelledByActorId,
            String clientRequestId,
            Instant occurredAt) implements SimulationEvent {
  }

  /**
   * Emitted when run completes successfully.
   */
  record SimulationCompleted(
      String simulationRunId,
      String networkId,
      int completedTicks,
      Instant occurredAt) implements SimulationEvent {
  }

  /**
   * Emitted when run fails.
   */
  record SimulationFailed(
      String simulationRunId,
      String networkId,
      String reason,
      Instant occurredAt) implements SimulationEvent {
  }
}
