package com.whispernetwork.api.application.services.simulation;

import com.whispernetwork.api.application.dto.SimulationTimelineEventSnapshot;
import com.whispernetwork.api.application.ports.out.SimulationEventStreamPort;
import com.whispernetwork.api.application.ports.out.SimulationProjectionStorePort;
import com.whispernetwork.api.application.ports.out.StartAcknowledgementPort;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Applies simulation domain events into API projections and live streams.
 */
@Service
public class SimulationEventProjectionApplicationService {
    private static final Logger logger = LoggerFactory.getLogger(SimulationEventProjectionApplicationService.class);

    private final SimulationProjectionStorePort projectionStore;
    private final SimulationEventStreamPort eventStream;
    private final StartAcknowledgementPort startAcknowledgement;

    /**
     * Creates event projection application service.
     */
    public SimulationEventProjectionApplicationService(
            SimulationProjectionStorePort projectionStore,
            SimulationEventStreamPort eventStream,
            StartAcknowledgementPort startAcknowledgement) {
        this.projectionStore = projectionStore;
        this.eventStream = eventStream;
        this.startAcknowledgement = startAcknowledgement;
    }

    /**
     * Applies a started event.
     */
    public void onStarted(
            String clientRequestId, String runId, String networkId, int networkVersionNumber, Instant startedAt) {
        String ownerId = resolveOwnerIdByTracking(clientRequestId);
        if (ownerId == null) {
            return;
        }

        projectionStore.upsertStarted(ownerId, clientRequestId, runId, networkId, networkVersionNumber, startedAt);

        SimulationTimelineEventSnapshot timelineEvent = new SimulationTimelineEventSnapshot(
                "STARTED", runId, networkId, null, null, null, null, null, null, null, null, List.of(), null, null,
                null, startedAt);

        projectionStore.appendTimelineForRun(ownerId, runId, timelineEvent);
        broadcast(ownerId, runId, clientRequestId, timelineEvent);
        startAcknowledgement.completeIfPending(clientRequestId, runId);
    }

    /**
     * Applies a cancelled event.
     */
    public void onCancelled(String runId, String networkId, Instant cancelledAt) {
        String ownerId = resolveOwnerIdByRunId(runId);
        if (ownerId == null) {
            return;
        }

        projectionStore.upsertCancelled(ownerId, runId, networkId, cancelledAt);

        SimulationTimelineEventSnapshot timelineEvent = new SimulationTimelineEventSnapshot(
                "CANCELLED",
                runId,
                networkId,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                null,
                null,
                null,
                cancelledAt);

        projectionStore.appendTimelineForRun(ownerId, runId, timelineEvent);
        broadcast(
                ownerId,
                runId,
                projectionStore.resolveTrackingId(ownerId, runId).orElse(null),
                timelineEvent);
    }

    /**
     * Applies a tick completed event.
     */
    public void onTickCompleted(String runId, String networkId, int tickNumber, int updatedAgents, Instant occurredAt) {
        String ownerId = resolveOwnerIdByRunId(runId);
        if (ownerId == null) {
            return;
        }

        projectionStore.upsertTickCompleted(ownerId, runId, networkId, tickNumber, occurredAt);

        SimulationTimelineEventSnapshot timelineEvent = new SimulationTimelineEventSnapshot(
                "TICK_COMPLETED",
                runId,
                networkId,
                tickNumber,
                updatedAgents,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                null,
                null,
                null,
                occurredAt);

        projectionStore.appendTimelineForRun(ownerId, runId, timelineEvent);
        broadcast(
                ownerId,
                runId,
                projectionStore.resolveTrackingId(ownerId, runId).orElse(null),
                timelineEvent);
    }

    /**
     * Applies a completed event.
     */
    public void onCompleted(String runId, String networkId, int completedTicks, Instant completedAt) {
        String ownerId = resolveOwnerIdByRunId(runId);
        if (ownerId == null) {
            return;
        }

        projectionStore.upsertCompleted(ownerId, runId, networkId, completedTicks, completedAt);

        SimulationTimelineEventSnapshot timelineEvent = new SimulationTimelineEventSnapshot(
                "COMPLETED",
                runId,
                networkId,
                null,
                null,
                completedTicks,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                null,
                null,
                null,
                completedAt);

        projectionStore.appendTimelineForRun(ownerId, runId, timelineEvent);
        broadcast(
                ownerId,
                runId,
                projectionStore.resolveTrackingId(ownerId, runId).orElse(null),
                timelineEvent);
    }

    /**
     * Applies a failed event.
     */
    public void onFailed(String runId, String networkId, String reason, Instant failedAt) {
        String ownerId = resolveOwnerIdByRunId(runId);
        if (ownerId == null) {
            return;
        }

        projectionStore.upsertFailed(ownerId, runId, networkId, reason, failedAt);

        SimulationTimelineEventSnapshot timelineEvent = new SimulationTimelineEventSnapshot(
                "FAILED", runId, networkId, null, null, null, reason, null, null, null, null, List.of(), null, null,
                null, failedAt);

        projectionStore.appendTimelineForRun(ownerId, runId, timelineEvent);
        broadcast(
                ownerId,
                runId,
                projectionStore.resolveTrackingId(ownerId, runId).orElse(null),
                timelineEvent);
    }

    /**
     * Applies an opinion update event.
     */
    public void onOpinionUpdated(
            String runId,
            String networkId,
            int tickNumber,
            String agentId,
            double previousOpinionValue,
            double newOpinionValue,
            double incomingInfluenceMagnitude,
            List<String> contributingSourceAgentIds,
            boolean relayedAsIs,
            String relayOriginAgentId,
            boolean ignoredTrustAndWeight,
            Instant occurredAt) {
        String ownerId = resolveOwnerIdByRunId(runId);
        if (ownerId == null) {
            return;
        }

        SimulationTimelineEventSnapshot timelineEvent = new SimulationTimelineEventSnapshot(
                "OPINION_UPDATED",
                runId,
                networkId,
                tickNumber,
                null,
                null,
                null,
                agentId,
                previousOpinionValue,
                newOpinionValue,
                incomingInfluenceMagnitude,
                contributingSourceAgentIds,
                relayedAsIs,
                relayOriginAgentId,
                ignoredTrustAndWeight,
                occurredAt);

        projectionStore.appendTimelineForRun(ownerId, runId, timelineEvent);
        broadcast(
                ownerId,
                runId,
                projectionStore.resolveTrackingId(ownerId, runId).orElse(null),
                timelineEvent);
    }

    private void broadcast(String ownerId, String runId, String trackingId, SimulationTimelineEventSnapshot event) {
        if (runId != null && !runId.isBlank()) {
            eventStream.publish(ownerId, runId, event);
        }
        if (trackingId != null && !trackingId.isBlank() && !trackingId.equals(runId)) {
            eventStream.publish(ownerId, trackingId, event);
        }
    }

    private String resolveOwnerIdByTracking(String trackingId) {
        return projectionStore.resolveOwnerIdByTrackingId(trackingId).orElseGet(() -> {
            logger.warn("Owner not found for tracking id {}", trackingId);
            // TODO(ownership): decide how to handle events when owner cannot be resolved
            return null;
        });
    }

    private String resolveOwnerIdByRunId(String runId) {
        return projectionStore.resolveOwnerIdByRunId(runId).orElseGet(() -> {
            logger.warn("Owner not found for run id {}", runId);
            // TODO(ownership): decide how to handle events when owner cannot be resolved
            return null;
        });
    }
}
