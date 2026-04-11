package com.whispernetwork.simulation.application.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Mutable run aggregate managing simulation lifecycle state.
 */
public final class SimulationRun {
  private final String id;
  private final String networkId;
  private final int networkVersionNumber;
  private final String requestedByActorId;
  private final String clientRequestId;
  private final int requestedTicks;
  private volatile SimulationRunStatus status;
  private volatile int completedTicks;
  private volatile String failureMessage;
  private volatile String cancellationRequestedByActorId;
  private volatile String cancellationClientRequestId;
  private final Instant createdAt;
  private volatile Instant updatedAt;

  /**
   * Creates a new run in REQUESTED state.
   */
  public SimulationRun(
      String id,
      String networkId,
      int networkVersionNumber,
      String requestedByActorId,
      String clientRequestId,
      int requestedTicks) {
    UUID.fromString(id);
    requireText(networkId, "networkId");
    requireText(requestedByActorId, "requestedByActorId");
    requireText(clientRequestId, "clientRequestId");
    this.id = id;
    this.networkId = networkId;
    this.networkVersionNumber = networkVersionNumber;
    this.requestedByActorId = requestedByActorId;
    this.clientRequestId = clientRequestId;
    this.requestedTicks = requestedTicks;
    this.status = SimulationRunStatus.REQUESTED;
    this.completedTicks = 0;
    this.failureMessage = null;
    this.cancellationRequestedByActorId = null;
    this.cancellationClientRequestId = null;
    this.createdAt = Instant.now();
    this.updatedAt = this.createdAt;
  }

  /**
   * Returns run id.
   */
  public String getId() {
    return id;
  }

  /**
   * Returns network id.
   */
  public String getNetworkId() {
    return networkId;
  }

  /**
   * Returns network version number.
   */
  public int getNetworkVersionNumber() {
    return networkVersionNumber;
  }

  /**
   * Returns requester actor id.
   */
  public String getRequestedByActorId() {
    return requestedByActorId;
  }

  /**
   * Returns start command idempotency key.
   */
  public String getClientRequestId() {
    return clientRequestId;
  }

  /**
   * Returns requested ticks.
   */
  public int getRequestedTicks() {
    return requestedTicks;
  }

  /**
   * Returns current status.
   */
  public SimulationRunStatus getStatus() {
    return status;
  }

  /**
   * Returns completed tick count.
   */
  public int getCompletedTicks() {
    return completedTicks;
  }

  /**
   * Returns failure message.
   */
  public String getFailureMessage() {
    return failureMessage;
  }

  /**
   * Returns actor id that requested cancellation, if any.
   */
  public String getCancellationRequestedByActorId() {
    return cancellationRequestedByActorId;
  }

  /**
   * Returns cancellation command idempotency key, if any.
   */
  public String getCancellationClientRequestId() {
    return cancellationClientRequestId;
  }

  /**
   * Returns creation timestamp.
   */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /**
   * Returns last update timestamp.
   */
  public Instant getUpdatedAt() {
    return updatedAt;
  }

  /**
   * Transitions REQUESTED to RUNNING.
   */
  public synchronized void markRunning() {
    if (status != SimulationRunStatus.REQUESTED) {
      throw new IllegalStateException("Run can only move to RUNNING from REQUESTED");
    }
    status = SimulationRunStatus.RUNNING;
    updatedAt = Instant.now();
  }

  /**
   * Transitions REQUESTED/RUNNING to CANCELLING.
   */
  public synchronized void markCancelling(String requestedByActorId, String clientRequestId) {
    requireText(requestedByActorId, "requestedByActorId");
    requireText(clientRequestId, "clientRequestId");
    if (status == SimulationRunStatus.REQUESTED || status == SimulationRunStatus.RUNNING) {
      status = SimulationRunStatus.CANCELLING;
      this.cancellationRequestedByActorId = requestedByActorId;
      this.cancellationClientRequestId = clientRequestId;
      updatedAt = Instant.now();
      return;
    }
    if (status.isTerminal() || status == SimulationRunStatus.CANCELLING) {
      return;
    }
    throw new IllegalStateException("Run cannot be marked CANCELLING from " + status);
  }

  /**
   * Increments completed tick count.
   */
  public synchronized void incrementCompletedTicks() {
    completedTicks++;
    updatedAt = Instant.now();
  }

  /**
   * Marks run completed.
   */
  public synchronized void markCompleted() {
    if (status != SimulationRunStatus.RUNNING) {
      throw new IllegalStateException("Run can only complete from RUNNING");
    }
    status = SimulationRunStatus.COMPLETED;
    updatedAt = Instant.now();
  }

  /**
   * Marks run cancelled.
   */
  public synchronized void markCancelled() {
    if (status != SimulationRunStatus.CANCELLING) {
      throw new IllegalStateException("Run can only cancel from CANCELLING");
    }
    status = SimulationRunStatus.CANCELLED;
    updatedAt = Instant.now();
  }

  /**
   * Marks run failed.
   */
  public synchronized void markFailed(String message) {
    status = SimulationRunStatus.FAILED;
    failureMessage = message;
    updatedAt = Instant.now();
  }

  private static void requireText(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " is required");
    }
  }
}
