package com.whispernetwork.api.infrastructure.persistence.repository;

import com.whispernetwork.api.infrastructure.persistence.entity.SimulationTimelineEventEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA repository for timeline events.
 */
public interface SimulationTimelineEventJpaRepository extends JpaRepository<SimulationTimelineEventEntity, Long> {
    List<SimulationTimelineEventEntity> findByOwnerIdAndRunIdOrderByOccurredAtAscIdAsc(String ownerId, String runId);

    List<SimulationTimelineEventEntity> findByOwnerIdAndTrackingIdOrderByOccurredAtAscIdAsc(
            String ownerId, String trackingId);

    long countByOwnerIdAndRunId(String ownerId, String runId);

    long countByOwnerIdAndTrackingId(String ownerId, String trackingId);
}
