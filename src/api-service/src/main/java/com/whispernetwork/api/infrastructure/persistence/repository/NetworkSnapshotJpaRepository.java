package com.whispernetwork.api.infrastructure.persistence.repository;

import com.whispernetwork.api.infrastructure.persistence.entity.NetworkSnapshotEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NetworkSnapshotJpaRepository extends JpaRepository<NetworkSnapshotEntity, Long> {
    Optional<NetworkSnapshotEntity> findByOwnerIdAndNetworkIdAndVersionNumber(
            String ownerId, String networkId, int versionNumber);

    Optional<NetworkSnapshotEntity> findFirstByOwnerIdAndNetworkIdOrderByVersionNumberDesc(
            String ownerId, String networkId);
}
