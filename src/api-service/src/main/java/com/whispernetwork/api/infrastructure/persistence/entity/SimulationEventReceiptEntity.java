package com.whispernetwork.api.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Tracks processed simulation events for idempotent receipt handling.
 */
@Entity
@Table(name = "simulation_event_receipts")
public class SimulationEventReceiptEntity {
    @Id
    @Column(name = "event_id", length = 64, nullable = false)
    private String eventId;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    public SimulationEventReceiptEntity() {}

    public SimulationEventReceiptEntity(String eventId, Instant receivedAt) {
        this.eventId = eventId;
        this.receivedAt = receivedAt;
    }

    public String getEventId() {
        return eventId;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }
}
