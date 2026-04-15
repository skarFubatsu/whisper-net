package com.whispernetwork.simulation.application.model;

import com.whispernetwork.shared.util.TextRequire;

/**
 * Command to cancel an active simulation run.
 */
public record SimulationCancelCommand(
        String simulationRunId, String networkId, String requestedByActorId, String clientRequestId) {

    /**
     * Validates command fields.
     */
    public SimulationCancelCommand {
        TextRequire.nonBlank(simulationRunId, "simulationRunId");
        TextRequire.nonBlank(networkId, "networkId");
        TextRequire.nonBlank(requestedByActorId, "requestedByActorId");
        TextRequire.nonBlank(clientRequestId, "clientRequestId");
    }
}
