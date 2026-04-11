package com.whispernetwork.simulation.application.model;

/**
 * Command to request a simulation run.
 */
public record SimulationStartCommand(
    String networkId,
    int networkVersionNumber,
    String requestedByActorId,
    String clientRequestId,
    int requestedTicks) {

  /**
   * Validates command fields.
   */
  public SimulationStartCommand {
    requireText(networkId, "networkId");
    requireText(requestedByActorId, "requestedByActorId");
    requireText(clientRequestId, "clientRequestId");
    if (networkVersionNumber < 1) {
      throw new IllegalArgumentException("networkVersionNumber must be >= 1");
    }
    if (requestedTicks < 1) {
      throw new IllegalArgumentException("requestedTicks must be >= 1");
    }
  }

  private static void requireText(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " is required");
    }
  }
}
