package com.whispernetwork.api.application.ports.out;

import java.util.concurrent.CompletableFuture;

/**
 * Tracks pending start acknowledgements while waiting for started events.
 */
public interface StartAcknowledgementPort {

    /**
     * Creates a pending acknowledgement future for a client request id.
     */
    CompletableFuture<String> createPending(String clientRequestId);

    /**
     * Completes a pending acknowledgement, if it exists.
     */
    void completeIfPending(String clientRequestId, String simulationRunId);

    /**
     * Clears any pending acknowledgement for the request id.
     */
    void clear(String clientRequestId);
}
