package com.whispernetwork.api.application.ports.out;

import com.whispernetwork.api.application.dto.SimulationRunSnapshot;
import com.whispernetwork.api.application.dto.SimulationTimelineEventSnapshot;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Persistence contract for simulation read models and timeline projections.
 */
public interface SimulationProjectionStorePort {

    /**
     * Saves an initial request projection keyed by tracking id.
     */
    void upsertRequested(
            String ownerId, String trackingId, String networkId, int networkVersionNumber, int requestedTicks);

    /**
     * Projects a started event and links tracking id to run id.
     */
    void upsertStarted(
            String ownerId,
            String trackingId,
            String runId,
            String networkId,
            int networkVersionNumber,
            Instant startedAt);

    /**
     * Projects a cancelled event.
     */
    void upsertCancelled(String ownerId, String runId, String networkId, Instant cancelledAt);

    /**
     * Projects a tick-completed event.
     */
    void upsertTickCompleted(String ownerId, String runId, String networkId, int tickNumber, Instant occurredAt);

    /**
     * Projects a completed event.
     */
    void upsertCompleted(String ownerId, String runId, String networkId, int completedTicks, Instant completedAt);

    /**
     * Projects a failed event.
     */
    void upsertFailed(String ownerId, String runId, String networkId, String reason, Instant failedAt);

    /**
     * Finds a simulation run by run id or tracking id.
     */
    Optional<SimulationRunSnapshot> findRun(String ownerId, String runOrTrackingId);

    /**
     * Resolves a run id from run id or tracking id.
     */
    Optional<String> resolveRunId(String ownerId, String runOrTrackingId);

    /**
     * Resolves tracking id from run id.
     */
    Optional<String> resolveTrackingId(String ownerId, String runId);

    /**
     * Resolves owner id from tracking id.
     */
    Optional<String> resolveOwnerIdByTrackingId(String trackingId);

    /**
     * Resolves owner id from run id.
     */
    Optional<String> resolveOwnerIdByRunId(String runId);

    /**
     * Appends a timeline event keyed by tracking id.
     */
    void appendTimelineForTracking(String ownerId, String trackingId, SimulationTimelineEventSnapshot event);

    /**
     * Appends a timeline event keyed by run id and mirrors to linked tracking id.
     */
    void appendTimelineForRun(String ownerId, String runId, SimulationTimelineEventSnapshot event);

    /**
     * Lists timeline events for a run id or tracking id.
     */
    List<SimulationTimelineEventSnapshot> listTimeline(String ownerId, String runOrTrackingId);
}
