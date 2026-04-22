package com.whispernetwork.api.infrastructure.persistence.repository;

import com.whispernetwork.api.infrastructure.persistence.entity.SimulationEventReceiptEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA repository for processed simulation event receipts.
 */
public interface SimulationEventReceiptJpaRepository extends JpaRepository<SimulationEventReceiptEntity, String> {}
