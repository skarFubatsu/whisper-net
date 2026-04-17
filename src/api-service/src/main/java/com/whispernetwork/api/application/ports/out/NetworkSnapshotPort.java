package com.whispernetwork.api.application.ports.out;

import com.whispernetwork.api.application.dto.catalog.NetworkSnapshotRecord;
import java.util.Optional;

public interface NetworkSnapshotPort {
    Optional<NetworkSnapshotRecord> findSnapshot(String ownerId, String networkId, int versionNumber);

    Optional<NetworkSnapshotRecord> findLatestSnapshot(String ownerId, String networkId);

    NetworkSnapshotRecord saveSnapshot(String ownerId, NetworkSnapshotRecord snapshot);
}
