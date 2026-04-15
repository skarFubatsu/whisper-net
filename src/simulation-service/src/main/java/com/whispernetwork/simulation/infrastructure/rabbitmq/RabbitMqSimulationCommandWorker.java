package com.whispernetwork.simulation.infrastructure.rabbitmq;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.whispernetwork.shared.messaging.RabbitTopology;
import com.whispernetwork.schema.v1.commands.SimulationCancelRequestedCommand;
import com.whispernetwork.schema.v1.commands.SimulationRequestedCommand;
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
import com.whispernetwork.simulation.infrastructure.sqlite.SimulationDatabase;

/**
 * RabbitMQ worker that translates command messages into orchestrator invocations.
 */
public final class RabbitMqSimulationCommandWorker implements AutoCloseable {
  private static final int DEFAULT_REQUESTED_TICKS = 10;

  private final SimulationOrchestrator orchestrator;
  private final OrmLiteSimulationRunRepository runRepository;
  private final OrmLiteSimulationHistoryRepository historyRepository;
  private final Connection connection;
  private final Channel commandChannel;
  private final Channel eventChannel;

  /**
   * Creates worker instance and initializes RabbitMQ consumers.
   */
  public RabbitMqSimulationCommandWorker() throws Exception {
    InMemoryInfluenceNetworkProvider networkProvider = new InMemoryInfluenceNetworkProvider();
    RuntimeNetworkFixtures.registerDefaults(networkProvider);

    SimulationDatabase database = new SimulationDatabase();
    database.migrate();

    this.connection = createConnection();
    this.commandChannel = connection.createChannel();
    this.eventChannel = connection.createChannel();

    declareTopology(commandChannel);
    declareTopology(eventChannel);

    this.runRepository = new OrmLiteSimulationRunRepository(database.jdbcUrl());
    this.historyRepository = new OrmLiteSimulationHistoryRepository(database.jdbcUrl());

    this.orchestrator = new SimulationOrchestrator(
        new SimulationTickEngine(new OpinionAggregator()),
      runRepository,
        networkProvider,
        new PersistingSimulationEventPublisher(
        historyRepository,
            new RabbitMqSimulationEventPublisher(eventChannel)));

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

    channel.queueDeclare(RabbitTopology.SIMULATION_QUEUE_REQUESTED, true, false, false, null);
    channel.queueDeclare(RabbitTopology.SIMULATION_QUEUE_CANCEL_REQUESTED, true, false, false, null);

    channel.queueBind(
      RabbitTopology.SIMULATION_QUEUE_REQUESTED,
      RabbitTopology.COMMANDS_EXCHANGE,
      RabbitTopology.COMMAND_ROUTING_SIMULATION_REQUESTED);
    channel.queueBind(
      RabbitTopology.SIMULATION_QUEUE_CANCEL_REQUESTED,
      RabbitTopology.COMMANDS_EXCHANGE,
      RabbitTopology.COMMAND_ROUTING_SIMULATION_CANCEL_REQUESTED);
  }

  private void configureConsumers() throws Exception {
    DeliverCallback requestedCallback = (consumerTag, message) -> {
      try {
        handleStart(message.getBody());
      } catch (RuntimeException parseOrDomainException) {
        System.err.println("Failed to process simulation.requested command: " + parseOrDomainException.getMessage());
      }
    };

    DeliverCallback cancelCallback = (consumerTag, message) -> {
      try {
        handleCancel(message.getBody());
      } catch (RuntimeException parseOrDomainException) {
        System.err.println("Failed to process simulation.cancel.requested command: " + parseOrDomainException.getMessage());
      }
    };

    commandChannel.basicConsume(RabbitTopology.SIMULATION_QUEUE_REQUESTED, true, requestedCallback, consumerTag -> {
    });
    commandChannel.basicConsume(RabbitTopology.SIMULATION_QUEUE_CANCEL_REQUESTED, true, cancelCallback, consumerTag -> {
    });
  }

  private void handleStart(byte[] body) {
    SimulationRequestedCommand command = parseRequestedCommand(body);
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
