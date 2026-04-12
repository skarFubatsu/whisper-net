package com.whispernetwork.shared.messaging;

/**
 * Shared RabbitMQ topology constants for command and event routing.
 */
public final class RabbitTopology {
  public static final String COMMANDS_EXCHANGE = "simulation.commands";
  public static final String EVENTS_EXCHANGE = "simulation.events";

  public static final String COMMAND_ROUTING_SIMULATION_REQUESTED = "simulation.requested";
  public static final String COMMAND_ROUTING_SIMULATION_CANCEL_REQUESTED = "simulation.cancel.requested";

  public static final String EVENT_ROUTING_SIMULATION_STARTED = "simulation.started";
  public static final String EVENT_ROUTING_SIMULATION_CANCELLED = "simulation.cancelled";
  public static final String EVENT_ROUTING_SIMULATION_TICK_COMPLETED = "simulation.tick.completed";
  public static final String EVENT_ROUTING_SIMULATION_COMPLETED = "simulation.completed";
  public static final String EVENT_ROUTING_SIMULATION_FAILED = "simulation.failed";
  public static final String EVENT_ROUTING_OPINION_UPDATED = "simulation.opinion.updated";

  public static final String SIMULATION_QUEUE_REQUESTED = "simulation.command.requested";
  public static final String SIMULATION_QUEUE_CANCEL_REQUESTED = "simulation.command.cancel.requested";

  private RabbitTopology() {
  }
}
