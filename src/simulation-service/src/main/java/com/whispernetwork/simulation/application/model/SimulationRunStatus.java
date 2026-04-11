package com.whispernetwork.simulation.application.model;

/**
 * Lifecycle statuses for simulation runs.
 */
public enum SimulationRunStatus {
  REQUESTED,
  RUNNING,
  CANCELLING,
  CANCELLED,
  COMPLETED,
  FAILED;

  /**
   * Returns whether the status is terminal.
   */
  public boolean isTerminal() {
    return this == CANCELLED || this == COMPLETED || this == FAILED;
  }
}
