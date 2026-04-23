package com.whispernetwork.api.infrastructure.persistence;

import com.whispernetwork.api.application.ports.out.NetworkVersionQueryPort;
import com.whispernetwork.api.infrastructure.persistence.repository.NetworkVersionJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class NetworkVersionQueryAdapter implements NetworkVersionQueryPort {
    private final NetworkVersionJpaRepository versionRepo;

    public NetworkVersionQueryAdapter(NetworkVersionJpaRepository versionRepo) {
        this.versionRepo = versionRepo;
    }

    @Override
    public boolean exists(String ownerId, String networkId, int versionNumber) {
        return versionRepo
                .findByOwnerIdAndNetworkIdAndVersionNumber(ownerId, networkId, versionNumber)
                .isPresent();
    }
}
