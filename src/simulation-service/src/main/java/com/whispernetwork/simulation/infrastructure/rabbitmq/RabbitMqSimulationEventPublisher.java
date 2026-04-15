package com.whispernetwork.simulation.infrastructure.rabbitmq;

import com.rabbitmq.client.Channel;
import com.whispernetwork.shared.messaging.RabbitTopology;
import com.whispernetwork.schema.v1.events.SimulationCancelledEvent;
import com.whispernetwork.schema.v1.events.SimulationCompletedEvent;
import com.whispernetwork.schema.v1.events.SimulationFailedEvent;
import com.whispernetwork.schema.v1.events.SimulationStartedEvent;
import com.whispernetwork.schema.v1.events.SimulationTickCompletedEvent;
import com.whispernetwork.schema.v1.events.OpinionUpdatedEvent;
import com.whispernetwork.simulation.application.model.SimulationEvent;
import com.whispernetwork.simulation.core.engine.AgentOpinionUpdate;
import com.whispernetwork.simulation.application.port.out.SimulationEventPort;

/**
 * Publishes selected simulation lifecycle events to RabbitMQ.
 */
public final class RabbitMqSimulationEventPublisher implements SimulationEventPort {
  private final Channel channel;

  /**
   * Creates publisher instance.
   */
  public RabbitMqSimulationEventPublisher(Channel channel) {
    this.channel = channel;
  }

  @Override
  public void publish(SimulationEvent event) {
    try {
      if (event instanceof SimulationEvent.SimulationStarted started) {
        publishStarted(started);
      } else if (event instanceof SimulationEvent.SimulationCancelled cancelled) {
        publishCancelled(cancelled);
      } else if (event instanceof SimulationEvent.SimulationTickCompleted tickCompleted) {
        publishTickCompleted(tickCompleted);
      } else if (event instanceof SimulationEvent.SimulationCompleted completed) {
        publishCompleted(completed);
      } else if (event instanceof SimulationEvent.SimulationFailed failed) {
        publishFailed(failed);
      }
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to publish simulation event", ex);
    }
  }

  private void publishStarted(SimulationEvent.SimulationStarted started) throws Exception {
    SimulationStartedEvent outbound = SimulationStartedEvent.newBuilder()
        .setSimulationRunId(started.simulationRunId())
        .setNetworkId(started.networkId())
        .setNetworkVersionNumber(started.networkVersionNumber())
        .setInitiatedByActorId(started.initiatedByActorId())
        .setClientRequestId(started.clientRequestId())
        .setStartedAtEpochMillis(started.occurredAt().toEpochMilli())
        .build();

    synchronized (channel) {
      channel.basicPublish(
          RabbitTopology.EVENTS_EXCHANGE,
          RabbitTopology.EVENT_ROUTING_SIMULATION_STARTED,
          null,
          outbound.toByteArray());
    }
  }

  private void publishCancelled(SimulationEvent.SimulationCancelled cancelled) throws Exception {
    SimulationCancelledEvent outbound = SimulationCancelledEvent.newBuilder()
        .setSimulationRunId(cancelled.simulationRunId())
        .setNetworkId(cancelled.networkId())
        .setCancelledByActorId(cancelled.cancelledByActorId())
        .setClientRequestId(cancelled.clientRequestId())
        .setCancelledAtEpochMillis(cancelled.occurredAt().toEpochMilli())
        .build();

    synchronized (channel) {
      channel.basicPublish(
          RabbitTopology.EVENTS_EXCHANGE,
          RabbitTopology.EVENT_ROUTING_SIMULATION_CANCELLED,
          null,
          outbound.toByteArray());
    }
  }

  private void publishTickCompleted(SimulationEvent.SimulationTickCompleted tickCompleted) throws Exception {
    SimulationTickCompletedEvent outbound = SimulationTickCompletedEvent.newBuilder()
        .setSimulationRunId(tickCompleted.simulationRunId())
        .setNetworkId(tickCompleted.networkId())
        .setTickNumber(tickCompleted.tickNumber())
        .setUpdatedAgents(tickCompleted.updatedAgents())
        .setOccurredAtEpochMillis(tickCompleted.occurredAt().toEpochMilli())
        .build();

    synchronized (channel) {
      channel.basicPublish(
          RabbitTopology.EVENTS_EXCHANGE,
          RabbitTopology.EVENT_ROUTING_SIMULATION_TICK_COMPLETED,
          null,
          outbound.toByteArray());
    }

    for (AgentOpinionUpdate update : tickCompleted.updates()) {
      publishOpinionUpdated(tickCompleted, update);
    }
  }

  private void publishOpinionUpdated(SimulationEvent.SimulationTickCompleted tickCompleted, AgentOpinionUpdate update)
      throws Exception {
    OpinionUpdatedEvent.Builder outbound = OpinionUpdatedEvent.newBuilder()
        .setSimulationRunId(tickCompleted.simulationRunId())
        .setNetworkId(tickCompleted.networkId())
        .setTickNumber(tickCompleted.tickNumber())
        .setAgentId(update.agentId())
        .setPreviousOpinionValue(update.previousOpinionValue())
        .setNewOpinionValue(update.newOpinionValue())
        .setIncomingInfluenceMagnitude(update.incomingInfluenceMagnitude())
        .addAllContributingSourceAgentIds(update.contributingSourceAgentIds())
        .setRelayedAsIs(update.relayedAsIs())
        .setIgnoredTrustAndWeight(update.ignoredTrustAndWeight())
        .setOccurredAtEpochMillis(tickCompleted.occurredAt().toEpochMilli());

    if (update.relayOriginAgentId() != null) {
      outbound.setRelayOriginAgentId(update.relayOriginAgentId());
    }

    synchronized (channel) {
      channel.basicPublish(
          RabbitTopology.EVENTS_EXCHANGE,
          RabbitTopology.EVENT_ROUTING_OPINION_UPDATED,
          null,
          outbound.build().toByteArray());
    }
  }

  private void publishCompleted(SimulationEvent.SimulationCompleted completed) throws Exception {
    SimulationCompletedEvent outbound = SimulationCompletedEvent.newBuilder()
        .setSimulationRunId(completed.simulationRunId())
        .setNetworkId(completed.networkId())
        .setCompletedTicks(completed.completedTicks())
        .setCompletedAtEpochMillis(completed.occurredAt().toEpochMilli())
        .build();

    synchronized (channel) {
      channel.basicPublish(
          RabbitTopology.EVENTS_EXCHANGE,
          RabbitTopology.EVENT_ROUTING_SIMULATION_COMPLETED,
          null,
          outbound.toByteArray());
    }
  }

  private void publishFailed(SimulationEvent.SimulationFailed failed) throws Exception {
    SimulationFailedEvent outbound = SimulationFailedEvent.newBuilder()
        .setSimulationRunId(failed.simulationRunId())
        .setNetworkId(failed.networkId())
        .setReason(failed.reason())
        .setFailedAtEpochMillis(failed.occurredAt().toEpochMilli())
        .build();

    synchronized (channel) {
      channel.basicPublish(
          RabbitTopology.EVENTS_EXCHANGE,
          RabbitTopology.EVENT_ROUTING_SIMULATION_FAILED,
          null,
          outbound.toByteArray());
    }
  }
}
