package com.whispernetwork.api.infrastructure.stream;

import com.whispernetwork.api.application.ports.out.NetworkSsePublisherPort;
import org.springframework.stereotype.Component;

@Component
public class NetworkSsePublisherAdapter implements NetworkSsePublisherPort {
    private final NetworkSseBroadcaster broadcaster;

    public NetworkSsePublisherAdapter(NetworkSseBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    @Override
    public void publishEvent(String ownerId, String networkId, String type, Object payload) {
        broadcaster.publishEvent(ownerId, networkId, type, payload);
    }
}
