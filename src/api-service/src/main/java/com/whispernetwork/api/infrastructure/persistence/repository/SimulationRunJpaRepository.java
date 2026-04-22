package com.whispernetwork.api.infrastructure.persistence.repository;

import com.whispernetwork.api.infrastructure.persistence.entity.SimulationRunEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA repository for simulation runs.
 */
public interface SimulationRunJpaRepository extends JpaRepository<SimulationRunEntity, Long> {
    Optional<SimulationRunEntity> findByTrackingId(String trackingId);

    Optional<SimulationRunEntity> findByRunId(String runId);

    Optional<SimulationRunEntity> findByOwnerIdAndRunId(String ownerId, String runId);

    Optional<SimulationRunEntity> findByOwnerIdAndTrackingId(String ownerId, String trackingId);
}
