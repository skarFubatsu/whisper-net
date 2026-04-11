package com.whispernetwork.simulation.application.model;

/**
 * Command to cancel an active simulation run.
 */
public record SimulationCancelCommand(
    String simulationRunId,
  String networkId,
    String requestedByActorId,
    String clientRequestId) {

  /**
   * Validates command fields.
   */
  public SimulationCancelCommand {
    requireText(simulationRunId, "simulationRunId");
    requireText(networkId, "networkId");
    requireText(requestedByActorId, "requestedByActorId");
    requireText(clientRequestId, "clientRequestId");
  }

  private static void requireText(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " is required");
    }
  }
}
