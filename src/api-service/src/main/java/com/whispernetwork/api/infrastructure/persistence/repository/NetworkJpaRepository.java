package com.whispernetwork.api.infrastructure.persistence.repository;

import com.whispernetwork.api.infrastructure.persistence.entity.NetworkEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NetworkJpaRepository extends JpaRepository<NetworkEntity, Long> {
    Optional<NetworkEntity> findByOwnerIdAndNetworkId(String ownerId, String networkId);

    List<NetworkEntity> findByOwnerId(String ownerId);

    boolean existsByOwnerIdAndNetworkId(String ownerId, String networkId);
}
