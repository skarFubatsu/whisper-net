package com.whispernetwork.simulation.application.model;

import com.whispernetwork.shared.util.TextRequire;

/**
 * Command to request a simulation run.
 */
public record SimulationStartCommand(
        String networkId,
        int networkVersionNumber,
        String requestedByActorId,
        String clientRequestId,
        int requestedTicks) {

    /**
     * Validates command fields.
     */
    public SimulationStartCommand {
        TextRequire.nonBlank(networkId, "networkId");
        TextRequire.nonBlank(requestedByActorId, "requestedByActorId");
        TextRequire.nonBlank(clientRequestId, "clientRequestId");
        if (networkVersionNumber < 1) {
            throw new IllegalArgumentException("networkVersionNumber must be >= 1");
        }
        if (requestedTicks < 1) {
            throw new IllegalArgumentException("requestedTicks must be >= 1");
        }
    }
}
