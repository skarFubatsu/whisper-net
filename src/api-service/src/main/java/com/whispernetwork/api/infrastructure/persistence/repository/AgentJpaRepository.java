package com.whispernetwork.api.infrastructure.persistence.repository;

import com.whispernetwork.api.infrastructure.persistence.entity.AgentEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentJpaRepository extends JpaRepository<AgentEntity, Long> {
    Optional<AgentEntity> findByOwnerIdAndAgentId(String ownerId, String agentId);

    List<AgentEntity> findByOwnerId(String ownerId);

    Page<AgentEntity> findByOwnerId(String ownerId, Pageable pageable);

    Page<AgentEntity> findByOwnerIdAndAgentIdIn(String ownerId, List<String> agentIds, Pageable pageable);

    Page<AgentEntity> findByOwnerIdAndAgentIdNotIn(String ownerId, List<String> agentIds, Pageable pageable);

    boolean existsByOwnerIdAndAgentId(String ownerId, String agentId);
}
