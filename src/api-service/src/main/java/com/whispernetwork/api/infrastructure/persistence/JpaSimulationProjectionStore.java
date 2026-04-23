package com.whispernetwork.api.infrastructure.persistence;

import com.whispernetwork.api.application.dto.SimulationRunSnapshot;
import com.whispernetwork.api.application.dto.SimulationTimelineEventSnapshot;
import com.whispernetwork.api.application.ports.out.SimulationProjectionStorePort;
import com.whispernetwork.api.infrastructure.persistence.entity.SimulationRunEntity;
import com.whispernetwork.api.infrastructure.persistence.entity.SimulationTimelineEventEntity;
import com.whispernetwork.api.infrastructure.persistence.repository.SimulationRunJpaRepository;
import com.whispernetwork.api.infrastructure.persistence.repository.SimulationTimelineEventJpaRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA-backed projection store for simulation runs and timelines.
 */
@Component
@Transactional
public class JpaSimulationProjectionStore implements SimulationProjectionStorePort {
    private static final int MAX_EVENTS_PER_RUN = 1_000;

    private final SimulationRunJpaRepository runRepository;
    private final SimulationTimelineEventJpaRepository eventRepository;

    public JpaSimulationProjectionStore(
            SimulationRunJpaRepository runRepository, SimulationTimelineEventJpaRepository eventRepository) {
        this.runRepository = runRepository;
        this.eventRepository = eventRepository;
    }

    @Override
    public void upsertRequested(
            String ownerId, String trackingId, String networkId, int networkVersionNumber, int requestedTicks) {
        Instant now = Instant.now();
        SimulationRunEntity entity = runRepository.findByTrackingId(trackingId).orElse(new SimulationRunEntity());
        enforceOwner(entity, ownerId);

        entity.setOwnerId(ownerId);
        entity.setRequestedByActorId(ownerId);
        entity.setTrackingId(trackingId);
        entity.setClientRequestId(trackingId);
        entity.setNetworkId(networkId);
        entity.setNetworkVersionNumber(networkVersionNumber);
        entity.setStatus("REQUESTED");
        entity.setCompletedTicks(0);
        entity.setRequestedTicks(requestedTicks);
        entity.setFailureMessage(null);
        entity.setCreatedAt(entity.getCreatedAt() == null ? now : entity.getCreatedAt());
        entity.setUpdatedAt(now);

        syncCompatibilityColumns(entity);
        runRepository.save(entity);
    }

    @Override
    public void upsertStarted(
            String ownerId,
            String trackingId,
            String runId,
            String networkId,
            int networkVersionNumber,
            Instant startedAt) {
        SimulationRunEntity existingByRun = runRepository.findByRunId(runId).orElse(null);
        if (existingByRun != null && isTerminalStatus(existingByRun.getStatus())) {
            enforceOwner(existingByRun, ownerId);
            if (existingByRun.getTrackingId() == null) {
                existingByRun.setTrackingId(trackingId);
            }
            if (existingByRun.getClientRequestId() == null) {
                existingByRun.setClientRequestId(trackingId);
            }
            existingByRun.setUpdatedAt(startedAt);
            syncCompatibilityColumns(existingByRun);
            runRepository.save(existingByRun);
            return;
        }

        SimulationRunEntity entity = runRepository.findByTrackingId(trackingId).orElse(new SimulationRunEntity());
        enforceOwner(entity, ownerId);

        int requestedTicks = entity.getRequestedTicks();
        Instant createdAt = entity.getCreatedAt() == null ? startedAt : entity.getCreatedAt();

        entity.setOwnerId(ownerId);
        entity.setRequestedByActorId(entity.getRequestedByActorId() == null ? ownerId : entity.getRequestedByActorId());
        entity.setTrackingId(trackingId);
        entity.setClientRequestId(entity.getClientRequestId() == null ? trackingId : entity.getClientRequestId());
        entity.setRunId(runId);
        entity.setNetworkId(networkId);
        entity.setNetworkVersionNumber(networkVersionNumber);
        entity.setStatus("RUNNING");
        entity.setCompletedTicks(Math.max(entity.getCompletedTicks(), 0));
        entity.setRequestedTicks(requestedTicks);
        entity.setFailureMessage(null);
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(startedAt);

        syncCompatibilityColumns(entity);
        runRepository.save(entity);
    }

    @Override
    public void upsertCancelled(String ownerId, String runId, String networkId, Instant cancelledAt) {
        SimulationRunEntity entity = resolveRun(ownerId, runId, networkId, cancelledAt);
        entity.setStatus("CANCELLED");
        entity.setUpdatedAt(cancelledAt);
        syncCompatibilityColumns(entity);
        runRepository.save(entity);
    }

    @Override
    public void upsertTickCompleted(
            String ownerId, String runId, String networkId, int tickNumber, Instant occurredAt) {
        SimulationRunEntity entity = resolveRun(ownerId, runId, networkId, occurredAt);
        if (isTerminalStatus(entity.getStatus())) {
            return;
        }

        entity.setStatus("RUNNING");
        entity.setCompletedTicks(Math.max(entity.getCompletedTicks(), tickNumber));
        entity.setUpdatedAt(occurredAt);
        syncCompatibilityColumns(entity);
        runRepository.save(entity);
    }

    @Override
    public void upsertCompleted(
            String ownerId, String runId, String networkId, int completedTicks, Instant completedAt) {
        SimulationRunEntity entity = resolveRun(ownerId, runId, networkId, completedAt);
        entity.setStatus("COMPLETED");
        entity.setCompletedTicks(completedTicks);
        entity.setUpdatedAt(completedAt);
        syncCompatibilityColumns(entity);
        runRepository.save(entity);
    }

    @Override
    public void upsertFailed(String ownerId, String runId, String networkId, String reason, Instant failedAt) {
        SimulationRunEntity entity = resolveRun(ownerId, runId, networkId, failedAt);
        entity.setStatus("FAILED");
        entity.setFailureMessage(reason);
        entity.setUpdatedAt(failedAt);
        syncCompatibilityColumns(entity);
        runRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SimulationRunSnapshot> findRun(String ownerId, String runOrTrackingId) {
        Optional<SimulationRunEntity> byRun = runRepository.findByOwnerIdAndRunId(ownerId, runOrTrackingId);
        if (byRun.isPresent()) {
            return byRun.map(this::toSnapshot);
        }

        return runRepository
                .findByOwnerIdAndTrackingId(ownerId, runOrTrackingId)
                .map(this::toSnapshot);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> resolveRunId(String ownerId, String runOrTrackingId) {
        if (runRepository.findByOwnerIdAndRunId(ownerId, runOrTrackingId).isPresent()) {
            return Optional.of(runOrTrackingId);
        }

        return runRepository
                .findByOwnerIdAndTrackingId(ownerId, runOrTrackingId)
                .map(SimulationRunEntity::getRunId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> resolveTrackingId(String ownerId, String runId) {
        return runRepository.findByOwnerIdAndRunId(ownerId, runId).map(SimulationRunEntity::getTrackingId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> resolveOwnerIdByTrackingId(String trackingId) {
        return runRepository.findByTrackingId(trackingId).map(SimulationRunEntity::getOwnerId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> resolveOwnerIdByRunId(String runId) {
        return runRepository.findByRunId(runId).map(SimulationRunEntity::getOwnerId);
    }

    @Override
    public void appendTimelineForTracking(String ownerId, String trackingId, SimulationTimelineEventSnapshot event) {
        SimulationTimelineEventEntity entity = toTimelineEntity(ownerId, event);
        entity.setTrackingId(trackingId);
        eventRepository.save(entity);
        trimTrackingEvents(ownerId, trackingId);
    }

    @Override
    public void appendTimelineForRun(String ownerId, String runId, SimulationTimelineEventSnapshot event) {
        SimulationTimelineEventEntity entity = toTimelineEntity(ownerId, event);
        entity.setRunId(runId);

        runRepository
                .findByOwnerIdAndRunId(ownerId, runId)
                .map(SimulationRunEntity::getTrackingId)
                .ifPresent(entity::setTrackingId);

        eventRepository.save(entity);
        trimRunEvents(ownerId, runId);
        if (entity.getTrackingId() != null) {
            trimTrackingEvents(ownerId, entity.getTrackingId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SimulationTimelineEventSnapshot> listTimeline(String ownerId, String runOrTrackingId) {
        List<SimulationTimelineEventEntity> byRun =
                eventRepository.findByOwnerIdAndRunIdOrderByOccurredAtAscIdAsc(ownerId, runOrTrackingId);
        if (!byRun.isEmpty()) {
            return byRun.stream().map(this::toSnapshot).toList();
        }

        List<SimulationTimelineEventEntity> byTracking =
                eventRepository.findByOwnerIdAndTrackingIdOrderByOccurredAtAscIdAsc(ownerId, runOrTrackingId);
        if (!byTracking.isEmpty()) {
            return byTracking.stream().map(this::toSnapshot).toList();
        }

        Optional<String> resolvedRunId = resolveRunId(ownerId, runOrTrackingId);
        if (resolvedRunId.isPresent() && !resolvedRunId.get().equals(runOrTrackingId)) {
            List<SimulationTimelineEventEntity> resolved =
                    eventRepository.findByOwnerIdAndRunIdOrderByOccurredAtAscIdAsc(ownerId, resolvedRunId.get());
            return resolved.stream().map(this::toSnapshot).toList();
        }

        return List.of();
    }

    private SimulationRunEntity resolveRun(String ownerId, String runId, String networkId, Instant occurredAt) {
        SimulationRunEntity entity = runRepository.findByRunId(runId).orElse(null);
        if (entity == null) {
            entity = new SimulationRunEntity();
            entity.setOwnerId(ownerId);
            entity.setRequestedByActorId(ownerId);
            entity.setRunId(runId);
            entity.setTrackingId(runId);
            entity.setClientRequestId(runId);
            entity.setNetworkId(networkId);
            entity.setNetworkVersionNumber(0);
            entity.setStatus("RUNNING");
            entity.setCompletedTicks(0);
            entity.setRequestedTicks(0);
            entity.setCreatedAt(occurredAt);
        } else {
            enforceOwner(entity, ownerId);
            if (entity.getNetworkId() == null) {
                entity.setNetworkId(networkId);
            }
            if (entity.getRequestedByActorId() == null) {
                entity.setRequestedByActorId(ownerId);
            }
            if (entity.getClientRequestId() == null) {
                entity.setClientRequestId(entity.getTrackingId() == null ? runId : entity.getTrackingId());
            }
        }

        entity.setUpdatedAt(occurredAt);
        return entity;
    }

    private void syncCompatibilityColumns(SimulationRunEntity entity) {
        if (entity.getRequestId() == null) {
            entity.setRequestId(entity.getClientRequestId());
        }

        Instant createdAt = entity.getCreatedAt();
        if (createdAt != null) {
            entity.setCreatedAtEpochMillis(createdAt.toEpochMilli());
        }

        Instant updatedAt = entity.getUpdatedAt();
        if (updatedAt != null) {
            entity.setUpdatedAtEpochMillis(updatedAt.toEpochMilli());
        }
    }

    private void enforceOwner(SimulationRunEntity entity, String ownerId) {
        if (entity.getOwnerId() == null) {
            return;
        }
        if (!ownerId.equals(entity.getOwnerId())) {
            throw new IllegalStateException("Owner mismatch for run tracking id " + entity.getTrackingId());
        }
    }

    private SimulationRunSnapshot toSnapshot(SimulationRunEntity entity) {
        return new SimulationRunSnapshot(
                entity.getRunId() == null ? entity.getTrackingId() : entity.getRunId(),
                entity.getNetworkId(),
                entity.getNetworkVersionNumber(),
                entity.getStatus(),
                entity.getCompletedTicks(),
                entity.getRequestedTicks(),
                entity.getFailureMessage(),
                entity.getCreatedAt() != null
                        ? entity.getCreatedAt()
                        : (entity.getCreatedAtEpochMillis() == null
                                ? null
                                : Instant.ofEpochMilli(entity.getCreatedAtEpochMillis())),
                entity.getUpdatedAt() != null
                        ? entity.getUpdatedAt()
                        : (entity.getUpdatedAtEpochMillis() == null
                                ? null
                                : Instant.ofEpochMilli(entity.getUpdatedAtEpochMillis())));
    }

    private SimulationTimelineEventEntity toTimelineEntity(String ownerId, SimulationTimelineEventSnapshot event) {
        SimulationTimelineEventEntity entity = new SimulationTimelineEventEntity();
        entity.setOwnerId(ownerId);
        entity.setEventType(event.eventType());
        entity.setNetworkId(event.networkId());
        entity.setTickNumber(event.tickNumber());
        entity.setUpdatedAgents(event.updatedAgents());
        entity.setCompletedTicks(event.completedTicks());
        entity.setReason(event.reason());
        entity.setAgentId(event.agentId());
        entity.setPreviousOpinionValue(event.previousOpinionValue());
        entity.setNewOpinionValue(event.newOpinionValue());
        entity.setIncomingInfluenceMagnitude(event.incomingInfluenceMagnitude());
        entity.setContributingSourceAgentIds(event.contributingSourceAgentIds());
        entity.setRelayedAsIs(event.relayedAsIs());
        entity.setRelayOriginAgentId(event.relayOriginAgentId());
        entity.setIgnoredTrustAndWeight(event.ignoredTrustAndWeight());
        entity.setOccurredAt(event.occurredAt());
        return entity;
    }

    private SimulationTimelineEventSnapshot toSnapshot(SimulationTimelineEventEntity entity) {
        return new SimulationTimelineEventSnapshot(
                entity.getEventType(),
                entity.getRunId(),
                entity.getNetworkId(),
                entity.getTickNumber(),
                entity.getUpdatedAgents(),
                entity.getCompletedTicks(),
                entity.getReason(),
                entity.getAgentId(),
                entity.getPreviousOpinionValue(),
                entity.getNewOpinionValue(),
                entity.getIncomingInfluenceMagnitude(),
                entity.getContributingSourceAgentIds(),
                entity.getRelayedAsIs(),
                entity.getRelayOriginAgentId(),
                entity.getIgnoredTrustAndWeight(),
                entity.getOccurredAt());
    }

    private static boolean isTerminalStatus(String status) {
        return "COMPLETED".equals(status) || "FAILED".equals(status) || "CANCELLED".equals(status);
    }

    private void trimRunEvents(String ownerId, String runId) {
        List<SimulationTimelineEventEntity> events =
                eventRepository.findByOwnerIdAndRunIdOrderByOccurredAtAscIdAsc(ownerId, runId);
        trimEvents(events);
    }

    private void trimTrackingEvents(String ownerId, String trackingId) {
        List<SimulationTimelineEventEntity> events =
                eventRepository.findByOwnerIdAndTrackingIdOrderByOccurredAtAscIdAsc(ownerId, trackingId);
        trimEvents(events);
    }

    private void trimEvents(List<SimulationTimelineEventEntity> events) {
        if (events.size() <= MAX_EVENTS_PER_RUN) {
            return;
        }

        int overflow = events.size() - MAX_EVENTS_PER_RUN;
        eventRepository.deleteAll(new ArrayList<>(events.subList(0, overflow)));
    }
}
