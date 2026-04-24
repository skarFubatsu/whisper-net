package com.whispernetwork.api.infrastructure.state;

import com.whispernetwork.api.application.dto.SimulationRunSnapshot;
import com.whispernetwork.api.application.dto.SimulationTimelineEventSnapshot;
import com.whispernetwork.api.application.ports.out.SimulationProjectionStorePort;
import java.time.Instant;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * In-memory projection store for run status and timeline events.
 */
@Component
@Profile("in-memory")
public class InMemorySimulationProjectionStore implements SimulationProjectionStorePort {
    private static final int MAX_EVENTS_PER_RUN = 1_000;

    private final Map<String, SimulationRunSnapshot> byRunId;
    private final Map<String, SimulationRunSnapshot> byTrackingId;
    private final Map<String, String> runIdByTrackingId;
    private final Map<String, String> trackingIdByRunId;
    private final Map<String, Deque<SimulationTimelineEventSnapshot>> timelineByRunId;
    private final Map<String, Deque<SimulationTimelineEventSnapshot>> timelineByTrackingId;

    public InMemorySimulationProjectionStore() {
        this.byRunId = new ConcurrentHashMap<>();
        this.byTrackingId = new ConcurrentHashMap<>();
        this.runIdByTrackingId = new ConcurrentHashMap<>();
        this.trackingIdByRunId = new ConcurrentHashMap<>();
        this.timelineByRunId = new ConcurrentHashMap<>();
        this.timelineByTrackingId = new ConcurrentHashMap<>();
    }

    @Override
    public void upsertRequested(
            String ownerId, String trackingId, String networkId, int networkVersionNumber, int requestedTicks) {
        byTrackingId.put(
                toKey(ownerId, trackingId),
                new SimulationRunSnapshot(
                        trackingId,
                        networkId,
                        networkVersionNumber,
                        "REQUESTED",
                        0,
                        requestedTicks,
                        null,
                        Instant.now(),
                        Instant.now()));
    }

    @Override
    public void upsertStarted(
            String ownerId,
            String trackingId,
            String runId,
            String networkId,
            int networkVersionNumber,
            Instant startedAt) {
        SimulationRunSnapshot existingByRun = byRunId.get(toKey(ownerId, runId));
        if (existingByRun != null && isTerminalStatus(existingByRun.status())) {
            byTrackingId.put(toKey(ownerId, trackingId), existingByRun);
            runIdByTrackingId.put(toKey(ownerId, trackingId), runId);
            trackingIdByRunId.put(toKey(ownerId, runId), trackingId);

            Deque<SimulationTimelineEventSnapshot> existingTrackingTimeline =
                    timelineByTrackingId.get(toKey(ownerId, trackingId));
            if (existingTrackingTimeline != null) {
                timelineByRunId.put(toKey(ownerId, runId), new ConcurrentLinkedDeque<>(existingTrackingTimeline));
            }
            return;
        }

        SimulationRunSnapshot requestedView = byTrackingId.get(toKey(ownerId, trackingId));
        int requestedTicks = requestedView == null ? 0 : requestedView.requestedTicks();
        Instant createdAt = requestedView == null ? startedAt : requestedView.createdAt();

        SimulationRunSnapshot view = new SimulationRunSnapshot(
                runId, networkId, networkVersionNumber, "RUNNING", 0, requestedTicks, null, createdAt, startedAt);

        byRunId.put(toKey(ownerId, runId), view);
        byTrackingId.put(toKey(ownerId, trackingId), view);
        runIdByTrackingId.put(toKey(ownerId, trackingId), runId);
        trackingIdByRunId.put(toKey(ownerId, runId), trackingId);

        Deque<SimulationTimelineEventSnapshot> existingTrackingTimeline =
                timelineByTrackingId.get(toKey(ownerId, trackingId));
        if (existingTrackingTimeline != null) {
            timelineByRunId.put(toKey(ownerId, runId), new ConcurrentLinkedDeque<>(existingTrackingTimeline));
        }
    }

    @Override
    public void upsertCancelled(String ownerId, String runId, String networkId, Instant cancelledAt) {
        SimulationRunSnapshot previous = byRunId.get(toKey(ownerId, runId));
        int requestedTicks = previous == null ? 0 : previous.requestedTicks();
        int completedTicks = previous == null ? 0 : previous.completedTicks();
        int networkVersionNumber = previous == null ? 0 : previous.networkVersionNumber();
        Instant createdAt = previous == null ? cancelledAt : previous.createdAt();

        SimulationRunSnapshot view = new SimulationRunSnapshot(
                runId,
                networkId,
                networkVersionNumber,
                "CANCELLED",
                completedTicks,
                requestedTicks,
                null,
                createdAt,
                cancelledAt);

        byRunId.put(toKey(ownerId, runId), view);
        Optional.ofNullable(trackingIdByRunId.get(toKey(ownerId, runId)))
                .ifPresent(trackingId -> byTrackingId.put(toKey(ownerId, trackingId), view));
    }

    @Override
    public void upsertTickCompleted(
            String ownerId, String runId, String networkId, int tickNumber, Instant occurredAt) {
        SimulationRunSnapshot previous = byRunId.get(toKey(ownerId, runId));
        if (previous != null && isTerminalStatus(previous.status())) {
            return;
        }

        int requestedTicks = previous == null ? 0 : previous.requestedTicks();
        int networkVersionNumber = previous == null ? 0 : previous.networkVersionNumber();
        Instant createdAt = previous == null ? occurredAt : previous.createdAt();

        SimulationRunSnapshot view = new SimulationRunSnapshot(
                runId,
                networkId,
                networkVersionNumber,
                "RUNNING",
                Math.max(previous == null ? 0 : previous.completedTicks(), tickNumber),
                requestedTicks,
                null,
                createdAt,
                occurredAt);

        byRunId.put(toKey(ownerId, runId), view);
        Optional.ofNullable(trackingIdByRunId.get(toKey(ownerId, runId)))
                .ifPresent(trackingId -> byTrackingId.put(toKey(ownerId, trackingId), view));
    }

    @Override
    public void upsertCompleted(
            String ownerId, String runId, String networkId, int completedTicks, Instant completedAt) {
        SimulationRunSnapshot previous = byRunId.get(toKey(ownerId, runId));
        int requestedTicks = previous == null ? completedTicks : previous.requestedTicks();
        int networkVersionNumber = previous == null ? 0 : previous.networkVersionNumber();
        Instant createdAt = previous == null ? completedAt : previous.createdAt();

        SimulationRunSnapshot view = new SimulationRunSnapshot(
                runId,
                networkId,
                networkVersionNumber,
                "COMPLETED",
                completedTicks,
                requestedTicks,
                null,
                createdAt,
                completedAt);

        byRunId.put(toKey(ownerId, runId), view);
        Optional.ofNullable(trackingIdByRunId.get(toKey(ownerId, runId)))
                .ifPresent(trackingId -> byTrackingId.put(toKey(ownerId, trackingId), view));
    }

    @Override
    public void upsertFailed(String ownerId, String runId, String networkId, String reason, Instant failedAt) {
        SimulationRunSnapshot previous = byRunId.get(toKey(ownerId, runId));
        int requestedTicks = previous == null ? 0 : previous.requestedTicks();
        int completedTicks = previous == null ? 0 : previous.completedTicks();
        int networkVersionNumber = previous == null ? 0 : previous.networkVersionNumber();
        Instant createdAt = previous == null ? failedAt : previous.createdAt();

        SimulationRunSnapshot view = new SimulationRunSnapshot(
                runId,
                networkId,
                networkVersionNumber,
                "FAILED",
                completedTicks,
                requestedTicks,
                reason,
                createdAt,
                failedAt);

        byRunId.put(toKey(ownerId, runId), view);
        Optional.ofNullable(trackingIdByRunId.get(toKey(ownerId, runId)))
                .ifPresent(trackingId -> byTrackingId.put(toKey(ownerId, trackingId), view));
    }

    @Override
    public Optional<SimulationRunSnapshot> findRun(String ownerId, String runOrTrackingId) {
        SimulationRunSnapshot byRun = byRunId.get(toKey(ownerId, runOrTrackingId));
        if (byRun != null) {
            return Optional.of(byRun);
        }

        SimulationRunSnapshot byTracking = byTrackingId.get(toKey(ownerId, runOrTrackingId));
        if (byTracking != null) {
            return Optional.of(byTracking);
        }

        return Optional.empty();
    }

    @Override
    public Optional<String> resolveRunId(String ownerId, String runOrTrackingId) {
        if (byRunId.containsKey(toKey(ownerId, runOrTrackingId))) {
            return Optional.of(runOrTrackingId);
        }

        return Optional.ofNullable(runIdByTrackingId.get(toKey(ownerId, runOrTrackingId)));
    }

    @Override
    public Optional<String> resolveTrackingId(String ownerId, String runId) {
        return Optional.ofNullable(trackingIdByRunId.get(toKey(ownerId, runId)));
    }

    @Override
    public Optional<String> resolveOwnerIdByTrackingId(String trackingId) {
        return byTrackingId.keySet().stream()
                .filter(key -> key.endsWith("::" + trackingId))
                .map(this::ownerFromKey)
                .findFirst();
    }

    @Override
    public Optional<String> resolveOwnerIdByRunId(String runId) {
        return byRunId.keySet().stream()
                .filter(key -> key.endsWith("::" + runId))
                .map(this::ownerFromKey)
                .findFirst();
    }

    @Override
    public void appendTimelineForTracking(String ownerId, String trackingId, SimulationTimelineEventSnapshot event) {
        appendWithLimit(
                timelineByTrackingId.computeIfAbsent(
                        toKey(ownerId, trackingId), ignored -> new ConcurrentLinkedDeque<>()),
                event);
    }

    @Override
    public void appendTimelineForRun(String ownerId, String runId, SimulationTimelineEventSnapshot event) {
        appendWithLimit(
                timelineByRunId.computeIfAbsent(toKey(ownerId, runId), ignored -> new ConcurrentLinkedDeque<>()),
                event);

        String trackingId = trackingIdByRunId.get(toKey(ownerId, runId));
        if (trackingId != null) {
            appendWithLimit(
                    timelineByTrackingId.computeIfAbsent(
                            toKey(ownerId, trackingId), ignored -> new ConcurrentLinkedDeque<>()),
                    event);
        }
    }

    @Override
    public List<SimulationTimelineEventSnapshot> listTimeline(String ownerId, String runOrTrackingId) {
        Deque<SimulationTimelineEventSnapshot> byRun = timelineByRunId.get(toKey(ownerId, runOrTrackingId));
        if (byRun != null) {
            return List.copyOf(byRun);
        }

        Deque<SimulationTimelineEventSnapshot> byTracking = timelineByTrackingId.get(toKey(ownerId, runOrTrackingId));
        if (byTracking != null) {
            return List.copyOf(byTracking);
        }

        String resolvedRunId = runIdByTrackingId.get(toKey(ownerId, runOrTrackingId));
        if (resolvedRunId != null) {
            Deque<SimulationTimelineEventSnapshot> resolved = timelineByRunId.get(toKey(ownerId, resolvedRunId));
            if (resolved != null) {
                return List.copyOf(resolved);
            }
        }

        return List.of();
    }

    private static boolean isTerminalStatus(String status) {
        return "COMPLETED".equals(status) || "FAILED".equals(status) || "CANCELLED".equals(status);
    }

    private static void appendWithLimit(
            Deque<SimulationTimelineEventSnapshot> events, SimulationTimelineEventSnapshot event) {
        events.addLast(event);
        while (events.size() > MAX_EVENTS_PER_RUN) {
            events.pollFirst();
        }
    }

    private static String toKey(String ownerId, String identifier) {
        return ownerId + "::" + identifier;
    }

    private String ownerFromKey(String key) {
        int separatorIndex = key.indexOf("::");
        if (separatorIndex < 0) {
            return key;
        }
        return key.substring(0, separatorIndex);
    }
}
