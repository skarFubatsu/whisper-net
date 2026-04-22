package com.whispernetwork.api.infrastructure.persistence.repository;

import com.whispernetwork.api.infrastructure.persistence.entity.NetworkVersionEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NetworkVersionJpaRepository extends JpaRepository<NetworkVersionEntity, Long> {
    List<NetworkVersionEntity> findByOwnerIdAndNetworkIdOrderByVersionNumberDesc(String ownerId, String networkId);

    Optional<NetworkVersionEntity> findByOwnerIdAndNetworkIdAndVersionNumber(
            String ownerId, String networkId, int versionNumber);

    int countByOwnerIdAndNetworkId(String ownerId, String networkId);
}
