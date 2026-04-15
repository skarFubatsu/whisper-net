package com.whispernetwork.simulation.application.model;

/**
 * Key used to deduplicate command processing.
 */
public record IdempotencyKey(String commandType, String actorId, String clientRequestId) {

    /**
     * Creates an idempotency key for a start command.
     */
    public static IdempotencyKey forStart(SimulationStartCommand command) {
        return new IdempotencyKey("SIMULATION_START", command.requestedByActorId(), command.clientRequestId());
    }

    /**
     * Creates an idempotency key for a cancel command.
     */
    public static IdempotencyKey forCancel(SimulationCancelCommand command) {
        return new IdempotencyKey("SIMULATION_CANCEL", command.requestedByActorId(), command.clientRequestId());
    }
}
