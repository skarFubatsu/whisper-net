package com.whispernetwork.simulation.application.model;

import com.whispernetwork.shared.util.TextRequire;
import com.whispernetwork.shared.dto.RunStatus;
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
  private volatile RunStatus status;
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
    this(
      id,
      networkId,
      networkVersionNumber,
      requestedByActorId,
      clientRequestId,
      requestedTicks,
      RunStatus.REQUESTED,
      0,
      null,
      null,
      null,
      Instant.now(),
      Instant.now());
    }

    private SimulationRun(
      String id,
      String networkId,
      int networkVersionNumber,
      String requestedByActorId,
      String clientRequestId,
      int requestedTicks,
      RunStatus status,
      int completedTicks,
      String failureMessage,
      String cancellationRequestedByActorId,
      String cancellationClientRequestId,
      Instant createdAt,
      Instant updatedAt) {
    UUID.fromString(id);
    TextRequire.nonBlank(networkId, "networkId");
    TextRequire.nonBlank(requestedByActorId, "requestedByActorId");
    TextRequire.nonBlank(clientRequestId, "clientRequestId");
    this.id = id;
    this.networkId = networkId;
    this.networkVersionNumber = networkVersionNumber;
    this.requestedByActorId = requestedByActorId;
    this.clientRequestId = clientRequestId;
    this.requestedTicks = requestedTicks;
    this.status = status;
    this.completedTicks = completedTicks;
    this.failureMessage = failureMessage;
    this.cancellationRequestedByActorId = cancellationRequestedByActorId;
    this.cancellationClientRequestId = cancellationClientRequestId;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  /**
   * Rehydrates a run aggregate from persisted storage.
   */
  public static SimulationRun rehydrate(
      String id,
      String networkId,
      int networkVersionNumber,
      String requestedByActorId,
      String clientRequestId,
      int requestedTicks,
      RunStatus status,
      int completedTicks,
      String failureMessage,
      String cancellationRequestedByActorId,
      String cancellationClientRequestId,
      Instant createdAt,
      Instant updatedAt) {
    UUID.fromString(id);
    TextRequire.nonBlank(networkId, "networkId");
    TextRequire.nonBlank(requestedByActorId, "requestedByActorId");
    TextRequire.nonBlank(clientRequestId, "clientRequestId");

    return new SimulationRun(
        id,
        networkId,
        networkVersionNumber,
        requestedByActorId,
        clientRequestId,
      requestedTicks,
      status,
      completedTicks,
      failureMessage,
      cancellationRequestedByActorId,
      cancellationClientRequestId,
      createdAt,
      updatedAt);
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
  public RunStatus getStatus() {
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
    if (status != RunStatus.REQUESTED) {
      throw new IllegalStateException("Run can only move to RUNNING from REQUESTED");
    }
    status = RunStatus.RUNNING;
    updatedAt = Instant.now();
  }

  /**
   * Transitions REQUESTED/RUNNING to CANCELLING.
   */
  public synchronized void markCancelling(String requestedByActorId, String clientRequestId) {
    TextRequire.nonBlank(requestedByActorId, "requestedByActorId");
    TextRequire.nonBlank(clientRequestId, "clientRequestId");
    if (status == RunStatus.REQUESTED || status == RunStatus.RUNNING) {
      status = RunStatus.CANCELLING;
      this.cancellationRequestedByActorId = requestedByActorId;
      this.cancellationClientRequestId = clientRequestId;
      updatedAt = Instant.now();
      return;
    }
    if (status.isTerminal() || status == RunStatus.CANCELLING) {
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
    if (status != RunStatus.RUNNING) {
      throw new IllegalStateException("Run can only complete from RUNNING");
    }
    status = RunStatus.COMPLETED;
    updatedAt = Instant.now();
  }

  /**
   * Marks run cancelled.
   */
  public synchronized void markCancelled() {
    if (status != RunStatus.CANCELLING) {
      throw new IllegalStateException("Run can only cancel from CANCELLING");
    }
    status = RunStatus.CANCELLED;
    updatedAt = Instant.now();
  }

  /**
   * Marks run failed.
   */
  public synchronized void markFailed(String message) {
    status = RunStatus.FAILED;
    failureMessage = message;
    updatedAt = Instant.now();
  }

}
