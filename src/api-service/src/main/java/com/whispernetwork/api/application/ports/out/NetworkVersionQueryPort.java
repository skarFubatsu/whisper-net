package com.whispernetwork.api.application.ports.out;

public interface NetworkVersionQueryPort {
    boolean exists(String ownerId, String networkId, int versionNumber);
}
