package com.whispernetwork.api.infrastructure.persistence;

import com.whispernetwork.api.application.dto.catalog.NetworkSnapshotRecord;
import com.whispernetwork.api.application.ports.out.NetworkSnapshotPort;
import com.whispernetwork.api.infrastructure.persistence.entity.NetworkSnapshotEntity;
import com.whispernetwork.api.infrastructure.persistence.repository.NetworkSnapshotJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class JpaNetworkSnapshotAdapter implements NetworkSnapshotPort {
    private final NetworkSnapshotJpaRepository snapshotRepo;

    public JpaNetworkSnapshotAdapter(NetworkSnapshotJpaRepository snapshotRepo) {
        this.snapshotRepo = snapshotRepo;
    }

    @Override
    public Optional<NetworkSnapshotRecord> findSnapshot(String ownerId, String networkId, int versionNumber) {
        return snapshotRepo
                .findByOwnerIdAndNetworkIdAndVersionNumber(ownerId, networkId, versionNumber)
                .map(this::toRecord);
    }

    @Override
    public Optional<NetworkSnapshotRecord> findLatestSnapshot(String ownerId, String networkId) {
        return snapshotRepo
                .findFirstByOwnerIdAndNetworkIdOrderByVersionNumberDesc(ownerId, networkId)
                .map(this::toRecord);
    }

    @Override
    public NetworkSnapshotRecord saveSnapshot(String ownerId, NetworkSnapshotRecord snapshot) {
        NetworkSnapshotEntity entity = snapshotRepo
                .findByOwnerIdAndNetworkIdAndVersionNumber(ownerId, snapshot.networkId(), snapshot.versionNumber())
                .orElseGet(NetworkSnapshotEntity::new);
        entity.setOwnerId(ownerId);
        entity.setNetworkId(snapshot.networkId());
        entity.setVersionNumber(snapshot.versionNumber());
        entity.setSnapshotJson(snapshot.snapshotJson());
        entity.setCreatedAt(snapshot.createdAt());
        return toRecord(snapshotRepo.save(entity));
    }

    private NetworkSnapshotRecord toRecord(NetworkSnapshotEntity entity) {
        return new NetworkSnapshotRecord(
                entity.getNetworkId(), entity.getVersionNumber(), entity.getSnapshotJson(), entity.getCreatedAt());
    }
}
