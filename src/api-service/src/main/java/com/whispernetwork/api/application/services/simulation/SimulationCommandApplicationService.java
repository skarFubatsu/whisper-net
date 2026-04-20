package com.whispernetwork.api.application.services.simulation;

import com.whispernetwork.api.application.dto.CancelSimulationCommand;
import com.whispernetwork.api.application.dto.SimulationTimelineEventSnapshot;
import com.whispernetwork.api.application.dto.StartSimulationCommand;
import com.whispernetwork.api.application.error.ForbiddenException;
import com.whispernetwork.api.application.error.NotFoundException;
import com.whispernetwork.api.application.ports.out.NetworkVersionQueryPort;
import com.whispernetwork.api.application.ports.out.SimulationCommandPublisherPort;
import com.whispernetwork.api.application.ports.out.SimulationProjectionStorePort;
import com.whispernetwork.api.application.ports.out.StartAcknowledgementPort;
import com.whispernetwork.api.application.security.ActorContext;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

/**
 * Handles simulation command use cases.
 */
@Service
public class SimulationCommandApplicationService {
    private static final long START_EVENT_WAIT_TIMEOUT_MILLIS = 2500L;

    private final SimulationCommandPublisherPort commandPublisher;
    private final SimulationProjectionStorePort projectionStore;
    private final StartAcknowledgementPort startAcknowledgement;
    private final NetworkVersionQueryPort networkVersionQuery;
    private final ActorContext actorContext;

    /**
     * Creates command application service.
     */
    public SimulationCommandApplicationService(
            SimulationCommandPublisherPort commandPublisher,
            SimulationProjectionStorePort projectionStore,
            StartAcknowledgementPort startAcknowledgement,
            NetworkVersionQueryPort networkVersionQuery,
            ActorContext actorContext) {
        this.commandPublisher = commandPublisher;
        this.projectionStore = projectionStore;
        this.startAcknowledgement = startAcknowledgement;
        this.networkVersionQuery = networkVersionQuery;
        this.actorContext = actorContext;
    }

    /**
     * Requests simulation start and returns run id when started event arrives, or tracking id fallback.
     */
    public String startSimulation(StartSimulationCommand command) {
        Instant now = Instant.now();
        String ownerId = requireOwnerMatch(command.ownerId(), command.actorId());
        boolean versionExists =
                networkVersionQuery.exists(ownerId, command.networkId(), command.networkVersionNumber());
        if (!versionExists) {
            throw new NotFoundException("Network version not found");
        }

        projectionStore.upsertRequested(
                ownerId,
                command.clientRequestId(),
                command.networkId(),
                command.networkVersionNumber(),
                command.requestedTicks());

        SimulationTimelineEventSnapshot requestedEvent = new SimulationTimelineEventSnapshot(
                "REQUESTED",
                null,
                command.networkId(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                null,
                null,
                null,
                now);

        projectionStore.appendTimelineForTracking(ownerId, command.clientRequestId(), requestedEvent);

        CompletableFuture<String> pendingAck = startAcknowledgement.createPending(command.clientRequestId());
        commandPublisher.publishStart(command);

        try {
            return pendingAck.get(START_EVENT_WAIT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (Exception ignored) {
            return command.clientRequestId();
        } finally {
            startAcknowledgement.clear(command.clientRequestId());
        }
    }

    /**
     * Requests cancellation for a run id or tracking id.
     */
    public boolean cancelSimulation(String runIdOrTrackingId, CancelSimulationCommand command) {
        String ownerId = requireOwnerMatch(command.ownerId(), command.actorId());
        Optional<String> resolvedRunId = projectionStore.resolveRunId(ownerId, runIdOrTrackingId);
        if (resolvedRunId.isEmpty()) {
            return false;
        }

        String runId = resolvedRunId.get();
        Optional<com.whispernetwork.api.application.dto.SimulationRunSnapshot> view =
                projectionStore.findRun(ownerId, runId);
        if (view.isEmpty()) {
            return false;
        }

        commandPublisher.publishCancel(runId, view.get().networkId(), command);
        return true;
    }

    private String requireOwnerMatch(String ownerId, String actorId) {
        String currentOwner = actorContext.currentActorId();
        if (!currentOwner.equals(ownerId)) {
            throw new ForbiddenException("Owner does not match current actor");
        }
        if (!ownerId.equals(actorId)) {
            throw new ForbiddenException("Actor must match owner");
        }
        return currentOwner;
    }
}
