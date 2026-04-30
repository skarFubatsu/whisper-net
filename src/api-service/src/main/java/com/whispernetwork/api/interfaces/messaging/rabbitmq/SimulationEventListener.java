package com.whispernetwork.api.interfaces.messaging.rabbitmq;

import com.google.protobuf.InvalidProtocolBufferException;
import com.whispernetwork.api.application.services.simulation.SimulationEventProjectionApplicationService;
import com.whispernetwork.api.application.services.simulation.SimulationEventReceiptApplicationService;
import com.whispernetwork.api.config.RabbitMqTopologyConfig;
import com.whispernetwork.schema.v1.events.OpinionUpdatedEvent;
import com.whispernetwork.schema.v1.events.SimulationCancelledEvent;
import com.whispernetwork.schema.v1.events.SimulationCompletedEvent;
import com.whispernetwork.schema.v1.events.SimulationFailedEvent;
import com.whispernetwork.schema.v1.events.SimulationStartedEvent;
import com.whispernetwork.schema.v1.events.SimulationTickCompletedEvent;
import java.time.Instant;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ event listener that maps wire messages into application projection calls.
 */
@Component
public class SimulationEventListener {
    private final SimulationEventReceiptApplicationService eventReceiptService;
    private final SimulationEventProjectionApplicationService eventProjectionService;

    public SimulationEventListener(
            SimulationEventReceiptApplicationService eventReceiptService,
            SimulationEventProjectionApplicationService eventProjectionService) {
        this.eventReceiptService = eventReceiptService;
        this.eventProjectionService = eventProjectionService;
    }

    @RabbitListener(queues = RabbitMqTopologyConfig.API_QUEUE_SIMULATION_STARTED)
    public void onSimulationStarted(byte[] body) {
        try {
            SimulationStartedEvent event = SimulationStartedEvent.parseFrom(body);
            if (!eventReceiptService.markReceivedIfNew(event.getEventId())) {
                return;
            }
            eventProjectionService.onStarted(
                    event.getClientRequestId(),
                    event.getSimulationRunId(),
                    event.getNetworkId(),
                    event.getNetworkVersionNumber(),
                    Instant.ofEpochMilli(event.getStartedAtEpochMillis()));
        } catch (InvalidProtocolBufferException ignored) {
        }
    }

    @RabbitListener(queues = RabbitMqTopologyConfig.API_QUEUE_SIMULATION_CANCELLED)
    public void onSimulationCancelled(byte[] body) {
        try {
            SimulationCancelledEvent event = SimulationCancelledEvent.parseFrom(body);
            if (!eventReceiptService.markReceivedIfNew(event.getEventId())) {
                return;
            }
            eventProjectionService.onCancelled(
                    event.getSimulationRunId(),
                    event.getNetworkId(),
                    Instant.ofEpochMilli(event.getCancelledAtEpochMillis()));
        } catch (InvalidProtocolBufferException ignored) {
        }
    }

    @RabbitListener(queues = RabbitMqTopologyConfig.API_QUEUE_SIMULATION_TICK_COMPLETED)
    public void onSimulationTickCompleted(byte[] body) {
        try {
            SimulationTickCompletedEvent event = SimulationTickCompletedEvent.parseFrom(body);
            if (!eventReceiptService.markReceivedIfNew(event.getEventId())) {
                return;
            }
            eventProjectionService.onTickCompleted(
                    event.getSimulationRunId(),
                    event.getNetworkId(),
                    event.getTickNumber(),
                    event.getUpdatedAgents(),
                    Instant.ofEpochMilli(event.getOccurredAtEpochMillis()));
        } catch (InvalidProtocolBufferException ignored) {
        }
    }

    @RabbitListener(queues = RabbitMqTopologyConfig.API_QUEUE_SIMULATION_COMPLETED)
    public void onSimulationCompleted(byte[] body) {
        try {
            SimulationCompletedEvent event = SimulationCompletedEvent.parseFrom(body);
            if (!eventReceiptService.markReceivedIfNew(event.getEventId())) {
                return;
            }
            eventProjectionService.onCompleted(
                    event.getSimulationRunId(),
                    event.getNetworkId(),
                    event.getCompletedTicks(),
                    Instant.ofEpochMilli(event.getCompletedAtEpochMillis()));
        } catch (InvalidProtocolBufferException ignored) {
        }
    }

    @RabbitListener(queues = RabbitMqTopologyConfig.API_QUEUE_SIMULATION_FAILED)
    public void onSimulationFailed(byte[] body) {
        try {
            SimulationFailedEvent event = SimulationFailedEvent.parseFrom(body);
            if (!eventReceiptService.markReceivedIfNew(event.getEventId())) {
                return;
            }
            eventProjectionService.onFailed(
                    event.getSimulationRunId(),
                    event.getNetworkId(),
                    event.getReason(),
                    Instant.ofEpochMilli(event.getFailedAtEpochMillis()));
        } catch (InvalidProtocolBufferException ignored) {
        }
    }

    @RabbitListener(queues = RabbitMqTopologyConfig.API_QUEUE_OPINION_UPDATED)
    public void onOpinionUpdated(byte[] body) {
        try {
            OpinionUpdatedEvent event = OpinionUpdatedEvent.parseFrom(body);
            if (!eventReceiptService.markReceivedIfNew(event.getEventId())) {
                return;
            }
            eventProjectionService.onOpinionUpdated(
                    event.getSimulationRunId(),
                    event.getNetworkId(),
                    event.getTickNumber(),
                    event.getAgentId(),
                    event.getPreviousOpinionValue(),
                    event.getNewOpinionValue(),
                    event.getIncomingInfluenceMagnitude(),
                    event.getContributingSourceAgentIdsList(),
                    event.getRelayedAsIs(),
                    event.getRelayOriginAgentId(),
                    event.getIgnoredTrustAndWeight(),
                    Instant.ofEpochMilli(event.getOccurredAtEpochMillis()));
        } catch (InvalidProtocolBufferException ignored) {
        }
    }
}
