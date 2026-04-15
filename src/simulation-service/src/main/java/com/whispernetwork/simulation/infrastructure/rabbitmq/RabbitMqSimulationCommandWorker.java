package com.whispernetwork.simulation.infrastructure.rabbitmq;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.whispernetwork.schema.v1.commands.SimulationCancelRequestedCommand;
import com.whispernetwork.schema.v1.commands.SimulationRequestedCommand;
import com.whispernetwork.shared.messaging.RabbitTopology;
import com.whispernetwork.simulation.application.model.SimulationCancelCommand;
import com.whispernetwork.simulation.application.model.SimulationStartCommand;
import com.whispernetwork.simulation.application.service.SimulationOrchestrator;
import com.whispernetwork.simulation.core.engine.OpinionAggregator;
import com.whispernetwork.simulation.core.engine.SimulationTickEngine;
import com.whispernetwork.simulation.infrastructure.inmemory.InMemoryInfluenceNetworkProvider;
import com.whispernetwork.simulation.infrastructure.inmemory.RuntimeNetworkFixtures;
import com.whispernetwork.simulation.infrastructure.sqlite.OrmLiteSimulationHistoryRepository;
import com.whispernetwork.simulation.infrastructure.sqlite.OrmLiteSimulationRunRepository;
import com.whispernetwork.simulation.infrastructure.sqlite.PersistingSimulationEventPublisher;
import com.whispernetwork.simulation.infrastructure.sqlite.SimulationCommandReceiptService;
import com.whispernetwork.simulation.infrastructure.sqlite.SimulationDatabase;
import java.util.Map;

/**
 * RabbitMQ worker that translates command messages into orchestrator invocations.
 */
public final class RabbitMqSimulationCommandWorker implements AutoCloseable {
    private static final int DEFAULT_REQUESTED_TICKS = 10;

    private final SimulationOrchestrator orchestrator;
    private final OrmLiteSimulationRunRepository runRepository;
    private final OrmLiteSimulationHistoryRepository historyRepository;
    private final SimulationCommandReceiptService commandReceiptService;
    private final Connection connection;
    private final Channel commandChannel;
    private final Channel eventChannel;

    /**
     * Creates worker instance and initializes RabbitMQ consumers.
     */
    public RabbitMqSimulationCommandWorker() throws Exception {
        SimulationDatabase database = new SimulationDatabase();
        database.migrate();

        String snapshotDbUrl = readEnv("SNAPSHOT_DB_URL", "");
        if (snapshotDbUrl == null || snapshotDbUrl.isBlank()) {
            String simulationDbUrl = System.getenv("SIMULATION_DB_URL");
            if (simulationDbUrl != null && !simulationDbUrl.isBlank()) {
                snapshotDbUrl = database.jdbcUrl();
            }
        }

        com.whispernetwork.simulation.application.port.out.InfluenceNetworkProvider networkProvider;
        if (snapshotDbUrl != null && !snapshotDbUrl.isBlank()) {
            networkProvider = new com.whispernetwork.simulation.infrastructure.sqlite.SqliteInfluenceNetworkProvider(
                    snapshotDbUrl);
        } else {
            InMemoryInfluenceNetworkProvider mem = new InMemoryInfluenceNetworkProvider();
            RuntimeNetworkFixtures.registerDefaults(mem);
            networkProvider = mem;
        }

        this.connection = createConnection();
        this.commandChannel = connection.createChannel();
        this.eventChannel = connection.createChannel();

        declareTopology(commandChannel);
        declareTopology(eventChannel);

        this.runRepository = new OrmLiteSimulationRunRepository(database.jdbcUrl());
        this.historyRepository = new OrmLiteSimulationHistoryRepository(database.jdbcUrl());
        this.commandReceiptService = new SimulationCommandReceiptService(database.jdbcUrl());

        this.orchestrator = new SimulationOrchestrator(
                new SimulationTickEngine(new OpinionAggregator()),
                runRepository,
                networkProvider,
                new PersistingSimulationEventPublisher(
                        historyRepository, new RabbitMqSimulationEventPublisher(eventChannel)));

        configureConsumers();
    }

    private static Connection createConnection() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(readEnv("RABBITMQ_HOST", "localhost"));
        factory.setPort(readEnvInt("RABBITMQ_PORT", 5672));
        factory.setUsername(readEnv("RABBITMQ_USERNAME", "guest"));
        factory.setPassword(readEnv("RABBITMQ_PASSWORD", "guest"));
        factory.setAutomaticRecoveryEnabled(true);
        factory.setTopologyRecoveryEnabled(true);
        return factory.newConnection("simulation-service-worker");
    }

    private static void declareTopology(Channel channel) throws Exception {
        channel.exchangeDeclare(RabbitTopology.COMMANDS_EXCHANGE, BuiltinExchangeType.DIRECT, true);
        channel.exchangeDeclare(RabbitTopology.EVENTS_EXCHANGE, BuiltinExchangeType.DIRECT, true);
        channel.exchangeDeclare(RabbitTopology.COMMANDS_DEAD_LETTER_EXCHANGE, BuiltinExchangeType.DIRECT, true);

        Map<String, Object> requestedArgs = Map.of(
                "x-dead-letter-exchange", RabbitTopology.COMMANDS_DEAD_LETTER_EXCHANGE,
                "x-dead-letter-routing-key", RabbitTopology.COMMAND_ROUTING_SIMULATION_REQUESTED_DEAD_LETTER);
        Map<String, Object> cancelArgs = Map.of(
                "x-dead-letter-exchange", RabbitTopology.COMMANDS_DEAD_LETTER_EXCHANGE,
                "x-dead-letter-routing-key", RabbitTopology.COMMAND_ROUTING_SIMULATION_CANCEL_REQUESTED_DEAD_LETTER);

        channel.queueDeclare(RabbitTopology.SIMULATION_QUEUE_REQUESTED, true, false, false, requestedArgs);
        channel.queueDeclare(RabbitTopology.SIMULATION_QUEUE_CANCEL_REQUESTED, true, false, false, cancelArgs);
        channel.queueDeclare(RabbitTopology.SIMULATION_QUEUE_REQUESTED_DEAD_LETTER, true, false, false, null);
        channel.queueDeclare(RabbitTopology.SIMULATION_QUEUE_CANCEL_REQUESTED_DEAD_LETTER, true, false, false, null);

        channel.queueBind(
                RabbitTopology.SIMULATION_QUEUE_REQUESTED,
                RabbitTopology.COMMANDS_EXCHANGE,
                RabbitTopology.COMMAND_ROUTING_SIMULATION_REQUESTED);
        channel.queueBind(
                RabbitTopology.SIMULATION_QUEUE_CANCEL_REQUESTED,
                RabbitTopology.COMMANDS_EXCHANGE,
                RabbitTopology.COMMAND_ROUTING_SIMULATION_CANCEL_REQUESTED);
        channel.queueBind(
                RabbitTopology.SIMULATION_QUEUE_REQUESTED_DEAD_LETTER,
                RabbitTopology.COMMANDS_DEAD_LETTER_EXCHANGE,
                RabbitTopology.COMMAND_ROUTING_SIMULATION_REQUESTED_DEAD_LETTER);
        channel.queueBind(
                RabbitTopology.SIMULATION_QUEUE_CANCEL_REQUESTED_DEAD_LETTER,
                RabbitTopology.COMMANDS_DEAD_LETTER_EXCHANGE,
                RabbitTopology.COMMAND_ROUTING_SIMULATION_CANCEL_REQUESTED_DEAD_LETTER);
    }

    private void configureConsumers() throws Exception {
        DeliverCallback requestedCallback = (consumerTag, message) -> {
            long deliveryTag = message.getEnvelope().getDeliveryTag();
            try {
                handleStart(message.getBody());
                commandChannel.basicAck(deliveryTag, false);
            } catch (IllegalArgumentException parseOrValidationException) {
                commandChannel.basicReject(deliveryTag, false);
                System.err.println("Rejected simulation.requested command to dead-letter queue: "
                        + parseOrValidationException.getMessage());
            } catch (RuntimeException parseOrDomainException) {
                commandChannel.basicNack(deliveryTag, false, true);
                System.err.println(
                        "Failed to process simulation.requested command: " + parseOrDomainException.getMessage());
            }
        };

        DeliverCallback cancelCallback = (consumerTag, message) -> {
            long deliveryTag = message.getEnvelope().getDeliveryTag();
            try {
                handleCancel(message.getBody());
                commandChannel.basicAck(deliveryTag, false);
            } catch (IllegalArgumentException parseOrValidationException) {
                commandChannel.basicReject(deliveryTag, false);
                System.err.println("Rejected simulation.cancel.requested command to dead-letter queue: "
                        + parseOrValidationException.getMessage());
            } catch (RuntimeException parseOrDomainException) {
                commandChannel.basicNack(deliveryTag, false, true);
                System.err.println("Failed to process simulation.cancel.requested command: "
                        + parseOrDomainException.getMessage());
            }
        };

        commandChannel.basicConsume(
                RabbitTopology.SIMULATION_QUEUE_REQUESTED, false, requestedCallback, consumerTag -> {});
        commandChannel.basicConsume(
                RabbitTopology.SIMULATION_QUEUE_CANCEL_REQUESTED, false, cancelCallback, consumerTag -> {});
    }

    private void handleStart(byte[] body) {
        SimulationRequestedCommand command = parseRequestedCommand(body);
        if (!commandReceiptService.markReceivedIfNew(command.getEventId())) {
            return;
        }
        int requestedTicks = command.getRequestedTicks() > 0 ? command.getRequestedTicks() : DEFAULT_REQUESTED_TICKS;

        orchestrator.requestSimulation(new SimulationStartCommand(
                command.getNetworkId(),
                command.getNetworkVersionNumber(),
                command.getRequestedByActorId(),
                command.getClientRequestId(),
                requestedTicks));
    }

    private void handleCancel(byte[] body) {
        SimulationCancelRequestedCommand command = parseCancelCommand(body);
        if (!commandReceiptService.markReceivedIfNew(command.getEventId())) {
            return;
        }

        orchestrator.requestCancellation(new SimulationCancelCommand(
                command.getSimulationRunId(),
                command.getNetworkId(),
                command.getRequestedByActorId(),
                command.getClientRequestId()));
    }

    private static SimulationRequestedCommand parseRequestedCommand(byte[] body) {
        try {
            return SimulationRequestedCommand.parseFrom(body);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid SimulationRequestedCommand payload", ex);
        }
    }

    private static SimulationCancelRequestedCommand parseCancelCommand(byte[] body) {
        try {
            return SimulationCancelRequestedCommand.parseFrom(body);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid SimulationCancelRequestedCommand payload", ex);
        }
    }

    private static String readEnv(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static int readEnvInt(String key, int fallback) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    @Override
    public void close() throws Exception {
        orchestrator.close();
        runRepository.close();
        historyRepository.close();
        if (commandChannel.isOpen()) {
            commandChannel.close();
        }
        if (eventChannel.isOpen()) {
            eventChannel.close();
        }
        if (connection.isOpen()) {
            connection.close();
        }
    }
}
