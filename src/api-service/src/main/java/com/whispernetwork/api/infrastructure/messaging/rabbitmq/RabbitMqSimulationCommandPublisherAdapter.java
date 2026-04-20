package com.whispernetwork.api.infrastructure.messaging.rabbitmq;

import com.whispernetwork.api.application.dto.CancelSimulationCommand;
import com.whispernetwork.api.application.dto.StartSimulationCommand;
import com.whispernetwork.api.application.ports.out.SimulationCommandPublisherPort;
import com.whispernetwork.schema.v1.commands.SimulationCancelRequestedCommand;
import com.whispernetwork.schema.v1.commands.SimulationRequestedCommand;
import com.whispernetwork.shared.messaging.RabbitTopology;
import java.util.UUID;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ implementation of simulation command publisher port.
 */
@Component
public class RabbitMqSimulationCommandPublisherAdapter implements SimulationCommandPublisherPort {
    private final RabbitTemplate rabbitTemplate;

    /**
     * Creates publisher adapter.
     */
    public RabbitMqSimulationCommandPublisherAdapter(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publishStart(StartSimulationCommand command) {
        SimulationRequestedCommand message = SimulationRequestedCommand.newBuilder()
                .setNetworkId(command.networkId())
                .setNetworkVersionNumber(command.networkVersionNumber())
                .setRequestedByActorId(command.actorId())
                .setClientRequestId(command.clientRequestId())
                .setRequestedTicks(command.requestedTicks())
                .setEventId(UUID.randomUUID().toString())
                .build();

        rabbitTemplate.convertAndSend(
                RabbitTopology.COMMANDS_EXCHANGE,
                RabbitTopology.COMMAND_ROUTING_SIMULATION_REQUESTED,
                message.toByteArray());
    }

    @Override
    public void publishCancel(String runId, String networkId, CancelSimulationCommand command) {
        SimulationCancelRequestedCommand message = SimulationCancelRequestedCommand.newBuilder()
                .setSimulationRunId(runId)
                .setNetworkId(networkId)
                .setRequestedByActorId(command.actorId())
                .setClientRequestId(command.clientRequestId())
                .setEventId(UUID.randomUUID().toString())
                .build();

        rabbitTemplate.convertAndSend(
                RabbitTopology.COMMANDS_EXCHANGE,
                RabbitTopology.COMMAND_ROUTING_SIMULATION_CANCEL_REQUESTED,
                message.toByteArray());
    }
}
