package com.whispernetwork.simulation.infrastructure.rabbitmq;

import com.rabbitmq.client.Channel;
import com.whispernetwork.schema.v1.events.OpinionUpdatedEvent;
import com.whispernetwork.schema.v1.events.SimulationCancelledEvent;
import com.whispernetwork.schema.v1.events.SimulationCompletedEvent;
import com.whispernetwork.schema.v1.events.SimulationFailedEvent;
import com.whispernetwork.schema.v1.events.SimulationStartedEvent;
import com.whispernetwork.schema.v1.events.SimulationTickCompletedEvent;
import com.whispernetwork.shared.messaging.RabbitTopology;
import com.whispernetwork.simulation.application.model.SimulationEvent;
import com.whispernetwork.simulation.application.port.out.SimulationEventPort;
import com.whispernetwork.simulation.core.engine.AgentOpinionUpdate;
import java.util.UUID;

/**
 * Publishes selected simulation lifecycle events to RabbitMQ.
 */
public final class RabbitMqSimulationEventPublisher implements SimulationEventPort {
    private static final long PUBLISH_CONFIRM_TIMEOUT_MILLIS = 5000L;

    private final Channel channel;

    /**
     * Creates publisher instance.
     */
    public RabbitMqSimulationEventPublisher(Channel channel) {
        this.channel = channel;
        try {
            this.channel.confirmSelect();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to enable RabbitMQ publisher confirms", ex);
        }
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
        String eventId = UUID.randomUUID().toString();
        SimulationStartedEvent outbound = SimulationStartedEvent.newBuilder()
                .setSimulationRunId(started.simulationRunId())
                .setNetworkId(started.networkId())
                .setNetworkVersionNumber(started.networkVersionNumber())
                .setInitiatedByActorId(started.initiatedByActorId())
                .setClientRequestId(started.clientRequestId())
                .setStartedAtEpochMillis(started.occurredAt().toEpochMilli())
                .setEventId(eventId)
                .setOwnerId(started.ownerId())
                .setRequestId(started.requestId())
                .build();

        synchronized (channel) {
            publishWithConfirm(RabbitTopology.EVENT_ROUTING_SIMULATION_STARTED, outbound.toByteArray());
        }
    }

    private void publishCancelled(SimulationEvent.SimulationCancelled cancelled) throws Exception {
        String eventId = UUID.randomUUID().toString();
        SimulationCancelledEvent outbound = SimulationCancelledEvent.newBuilder()
                .setSimulationRunId(cancelled.simulationRunId())
                .setNetworkId(cancelled.networkId())
                .setCancelledByActorId(cancelled.cancelledByActorId())
                .setClientRequestId(cancelled.clientRequestId())
                .setCancelledAtEpochMillis(cancelled.occurredAt().toEpochMilli())
                .setEventId(eventId)
                .setOwnerId(cancelled.ownerId())
                .setRequestId(cancelled.requestId())
                .build();

        synchronized (channel) {
            publishWithConfirm(RabbitTopology.EVENT_ROUTING_SIMULATION_CANCELLED, outbound.toByteArray());
        }
    }

    private void publishTickCompleted(SimulationEvent.SimulationTickCompleted tickCompleted) throws Exception {
        String eventId = UUID.randomUUID().toString();
        SimulationTickCompletedEvent outbound = SimulationTickCompletedEvent.newBuilder()
                .setSimulationRunId(tickCompleted.simulationRunId())
                .setNetworkId(tickCompleted.networkId())
                .setTickNumber(tickCompleted.tickNumber())
                .setUpdatedAgents(tickCompleted.updatedAgents())
                .setOccurredAtEpochMillis(tickCompleted.occurredAt().toEpochMilli())
                .setEventId(eventId)
                .setOwnerId(tickCompleted.ownerId())
                .setRequestId(tickCompleted.requestId())
                .build();

        synchronized (channel) {
            publishWithConfirm(RabbitTopology.EVENT_ROUTING_SIMULATION_TICK_COMPLETED, outbound.toByteArray());
        }

        for (AgentOpinionUpdate update : tickCompleted.updates()) {
            publishOpinionUpdated(tickCompleted, update);
        }
    }

    private void publishOpinionUpdated(SimulationEvent.SimulationTickCompleted tickCompleted, AgentOpinionUpdate update)
            throws Exception {
        String eventId = UUID.randomUUID().toString();
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
                .setOccurredAtEpochMillis(tickCompleted.occurredAt().toEpochMilli())
                .setEventId(eventId)
                .setOwnerId(tickCompleted.ownerId())
                .setRequestId(tickCompleted.requestId());

        if (update.relayOriginAgentId() != null) {
            outbound.setRelayOriginAgentId(update.relayOriginAgentId());
        }

        synchronized (channel) {
            publishWithConfirm(
                    RabbitTopology.EVENT_ROUTING_OPINION_UPDATED,
                    outbound.build().toByteArray());
        }
    }

    private void publishCompleted(SimulationEvent.SimulationCompleted completed) throws Exception {
        String eventId = UUID.randomUUID().toString();
        SimulationCompletedEvent outbound = SimulationCompletedEvent.newBuilder()
                .setSimulationRunId(completed.simulationRunId())
                .setNetworkId(completed.networkId())
                .setCompletedTicks(completed.completedTicks())
                .setCompletedAtEpochMillis(completed.occurredAt().toEpochMilli())
                .setEventId(eventId)
                .setOwnerId(completed.ownerId())
                .setRequestId(completed.requestId())
                .build();

        synchronized (channel) {
            publishWithConfirm(RabbitTopology.EVENT_ROUTING_SIMULATION_COMPLETED, outbound.toByteArray());
        }
    }

    private void publishFailed(SimulationEvent.SimulationFailed failed) throws Exception {
        String eventId = UUID.randomUUID().toString();
        SimulationFailedEvent outbound = SimulationFailedEvent.newBuilder()
                .setSimulationRunId(failed.simulationRunId())
                .setNetworkId(failed.networkId())
                .setReason(failed.reason())
                .setFailedAtEpochMillis(failed.occurredAt().toEpochMilli())
                .setEventId(eventId)
                .setOwnerId(failed.ownerId())
                .setRequestId(failed.requestId())
                .build();

        synchronized (channel) {
            publishWithConfirm(RabbitTopology.EVENT_ROUTING_SIMULATION_FAILED, outbound.toByteArray());
        }
    }

    private void publishWithConfirm(String routingKey, byte[] payload) throws Exception {
        channel.basicPublish(RabbitTopology.EVENTS_EXCHANGE, routingKey, true, null, payload);
        boolean acknowledged = channel.waitForConfirms(PUBLISH_CONFIRM_TIMEOUT_MILLIS);
        if (!acknowledged) {
            throw new IllegalStateException("Broker did not confirm event publish for routing key: " + routingKey);
        }
    }
}
