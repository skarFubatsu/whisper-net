package com.whispernetwork.api.infrastructure.persistence;

import com.whispernetwork.api.application.dto.catalog.NetworkVersionView;
import com.whispernetwork.api.application.dto.catalog.NetworkView;
import com.whispernetwork.api.application.ports.out.NetworkCatalogPort;
import com.whispernetwork.api.infrastructure.persistence.entity.NetworkEntity;
import com.whispernetwork.api.infrastructure.persistence.entity.NetworkVersionEntity;
import com.whispernetwork.api.infrastructure.persistence.repository.NetworkJpaRepository;
import com.whispernetwork.api.infrastructure.persistence.repository.NetworkVersionJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class JpaNetworkCatalogAdapter implements NetworkCatalogPort {
    private final NetworkJpaRepository networkRepo;
    private final NetworkVersionJpaRepository versionRepo;

    public JpaNetworkCatalogAdapter(NetworkJpaRepository networkRepo, NetworkVersionJpaRepository versionRepo) {
        this.networkRepo = networkRepo;
        this.versionRepo = versionRepo;
    }

    @Override
    public NetworkView create(String ownerId, NetworkView network) {
        NetworkEntity entity = new NetworkEntity();
        entity.setOwnerId(ownerId);
        entity.setNetworkId(network.networkId());
        entity.setName(network.name());
        entity.setCreatedAt(network.createdAt());
        entity.setUpdatedAt(network.updatedAt());
        return toView(networkRepo.save(entity));
    }

    @Override
    public Optional<NetworkView> findByOwnerIdAndNetworkId(String ownerId, String networkId) {
        return networkRepo.findByOwnerIdAndNetworkId(ownerId, networkId).map(this::toView);
    }

    @Override
    public List<NetworkView> findByOwnerId(String ownerId) {
        return networkRepo.findByOwnerId(ownerId).stream().map(this::toView).collect(Collectors.toList());
    }

    @Override
    public int countVersions(String ownerId, String networkId) {
        return versionRepo.countByOwnerIdAndNetworkId(ownerId, networkId);
    }

    @Override
    public NetworkVersionView createVersion(String ownerId, NetworkVersionView version) {
        NetworkVersionEntity entity = new NetworkVersionEntity();
        entity.setOwnerId(ownerId);
        entity.setNetworkId(version.networkId());
        entity.setVersionNumber(version.versionNumber());
        entity.setDescription(version.description());
        entity.setCreatedBy(version.createdBy());
        entity.setCreatedAt(version.createdAt());
        return toView(versionRepo.save(entity));
    }

    @Override
    public Optional<NetworkVersionView> findVersion(String ownerId, String networkId, int versionNumber) {
        return versionRepo
                .findByOwnerIdAndNetworkIdAndVersionNumber(ownerId, networkId, versionNumber)
                .map(this::toView);
    }

    @Override
    public List<NetworkVersionView> listVersions(String ownerId, String networkId) {
        return versionRepo.findByOwnerIdAndNetworkIdOrderByVersionNumberDesc(ownerId, networkId).stream()
                .map(this::toView)
                .collect(Collectors.toList());
    }

    private NetworkView toView(NetworkEntity entity) {
        return new NetworkView(entity.getNetworkId(), entity.getName(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private NetworkVersionView toView(NetworkVersionEntity entity) {
        return new NetworkVersionView(
                entity.getNetworkId(),
                entity.getVersionNumber(),
                entity.getDescription(),
                entity.getCreatedBy(),
                entity.getCreatedAt());
    }
}
