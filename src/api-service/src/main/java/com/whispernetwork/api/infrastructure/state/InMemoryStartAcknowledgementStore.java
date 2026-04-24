package com.whispernetwork.api.infrastructure.state;

import com.whispernetwork.api.application.ports.out.StartAcknowledgementPort;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * In-memory store for pending start acknowledgements.
 */
@Component
public class InMemoryStartAcknowledgementStore implements StartAcknowledgementPort {
    private final Map<String, CompletableFuture<String>> pending;

    public InMemoryStartAcknowledgementStore() {
        this.pending = new ConcurrentHashMap<>();
    }

    @Override
    public CompletableFuture<String> createPending(String clientRequestId) {
        CompletableFuture<String> pendingFuture = new CompletableFuture<>();
        pending.put(clientRequestId, pendingFuture);
        return pendingFuture;
    }

    @Override
    public void completeIfPending(String clientRequestId, String simulationRunId) {
        CompletableFuture<String> pendingFuture = pending.get(clientRequestId);
        if (pendingFuture != null) {
            pendingFuture.complete(simulationRunId);
        }
    }

    @Override
    public void clear(String clientRequestId) {
        pending.remove(clientRequestId);
    }
}
