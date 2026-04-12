package com.whispernetwork.shared.dto;

/**
 * Shared lifecycle statuses for simulation run projections across services.
 */
public enum RunStatus {
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
