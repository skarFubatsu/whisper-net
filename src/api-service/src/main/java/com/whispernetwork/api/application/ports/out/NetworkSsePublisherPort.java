package com.whispernetwork.api.application.ports.out;

public interface NetworkSsePublisherPort {
    void publishEvent(String ownerId, String networkId, String type, Object payload);
}
