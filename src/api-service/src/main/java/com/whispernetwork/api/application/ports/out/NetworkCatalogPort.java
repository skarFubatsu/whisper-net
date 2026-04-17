package com.whispernetwork.api.application.ports.out;

import com.whispernetwork.api.application.dto.catalog.NetworkVersionView;
import com.whispernetwork.api.application.dto.catalog.NetworkView;
import java.util.List;
import java.util.Optional;

public interface NetworkCatalogPort {
    NetworkView create(String ownerId, NetworkView network);

    Optional<NetworkView> findByOwnerIdAndNetworkId(String ownerId, String networkId);

    List<NetworkView> findByOwnerId(String ownerId);

    int countVersions(String ownerId, String networkId);

    NetworkVersionView createVersion(String ownerId, NetworkVersionView version);

    Optional<NetworkVersionView> findVersion(String ownerId, String networkId, int versionNumber);

    List<NetworkVersionView> listVersions(String ownerId, String networkId);
}
