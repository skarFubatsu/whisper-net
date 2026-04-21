package com.whispernetwork.api.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/**
 * JPA entity for simulation run projections.
 */
@Entity
@Table(
        name = "simulation_runs",
        indexes = {
            @Index(name = "idx_sim_runs_owner_run", columnList = "owner_id, run_id"),
            @Index(name = "idx_sim_runs_owner_tracking", columnList = "owner_id, tracking_id")
        },
        uniqueConstraints = {
            @UniqueConstraint(name = "uq_sim_runs_tracking", columnNames = "tracking_id"),
            @UniqueConstraint(name = "uq_sim_runs_run", columnNames = "run_id")
        })
public class SimulationRunEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", length = 64, nullable = false)
    private String ownerId;

    @Column(name = "requested_by_actor_id", length = 64, nullable = false)
    private String requestedByActorId;

    @Column(name = "tracking_id", length = 64, nullable = false)
    private String trackingId;

    @Column(name = "client_request_id", length = 64, nullable = false)
    private String clientRequestId;

    @Column(name = "request_id", length = 64)
    private String requestId;

    @Column(name = "run_id", length = 64)
    private String runId;

    @Column(name = "network_id", length = 64, nullable = false)
    private String networkId;

    @Column(name = "network_version_number", nullable = false)
    private int networkVersionNumber;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "completed_ticks", nullable = false)
    private int completedTicks;

    @Column(name = "requested_ticks", nullable = false)
    private int requestedTicks;

    @Column(name = "failure_message", length = 500)
    private String failureMessage;

    @Column(name = "cancellation_requested_by_actor_id", length = 64)
    private String cancellationRequestedByActorId;

    @Column(name = "cancellation_client_request_id", length = 64)
    private String cancellationClientRequestId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_at_epoch_millis", nullable = false)
    private Long createdAtEpochMillis;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_at_epoch_millis", nullable = false)
    private Long updatedAtEpochMillis;

    public SimulationRunEntity() {}

    public Long getId() {
        return id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getRequestedByActorId() {
        return requestedByActorId;
    }

    public void setRequestedByActorId(String requestedByActorId) {
        this.requestedByActorId = requestedByActorId;
    }

    public String getTrackingId() {
        return trackingId;
    }

    public void setTrackingId(String trackingId) {
        this.trackingId = trackingId;
    }

    public String getClientRequestId() {
        return clientRequestId;
    }

    public void setClientRequestId(String clientRequestId) {
        this.clientRequestId = clientRequestId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getNetworkId() {
        return networkId;
    }

    public void setNetworkId(String networkId) {
        this.networkId = networkId;
    }

    public int getNetworkVersionNumber() {
        return networkVersionNumber;
    }

    public void setNetworkVersionNumber(int networkVersionNumber) {
        this.networkVersionNumber = networkVersionNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getCompletedTicks() {
        return completedTicks;
    }

    public void setCompletedTicks(int completedTicks) {
        this.completedTicks = completedTicks;
    }

    public int getRequestedTicks() {
        return requestedTicks;
    }

    public void setRequestedTicks(int requestedTicks) {
        this.requestedTicks = requestedTicks;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public void setFailureMessage(String failureMessage) {
        this.failureMessage = failureMessage;
    }

    public String getCancellationRequestedByActorId() {
        return cancellationRequestedByActorId;
    }

    public void setCancellationRequestedByActorId(String cancellationRequestedByActorId) {
        this.cancellationRequestedByActorId = cancellationRequestedByActorId;
    }

    public String getCancellationClientRequestId() {
        return cancellationClientRequestId;
    }

    public void setCancellationClientRequestId(String cancellationClientRequestId) {
        this.cancellationClientRequestId = cancellationClientRequestId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Long getCreatedAtEpochMillis() {
        return createdAtEpochMillis;
    }

    public void setCreatedAtEpochMillis(Long createdAtEpochMillis) {
        this.createdAtEpochMillis = createdAtEpochMillis;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getUpdatedAtEpochMillis() {
        return updatedAtEpochMillis;
    }

    public void setUpdatedAtEpochMillis(Long updatedAtEpochMillis) {
        this.updatedAtEpochMillis = updatedAtEpochMillis;
    }
}
