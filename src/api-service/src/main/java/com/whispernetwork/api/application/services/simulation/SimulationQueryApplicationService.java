package com.whispernetwork.api.application.services.simulation;

import com.whispernetwork.api.application.dto.SimulationRunSnapshot;
import com.whispernetwork.api.application.dto.SimulationTimelineEventSnapshot;
import com.whispernetwork.api.application.ports.out.SimulationProjectionStorePort;
import com.whispernetwork.api.application.security.ActorContext;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Handles simulation query and streaming use cases.
 */
@Service
public class SimulationQueryApplicationService {
    private final SimulationProjectionStorePort projectionStore;
    private final ActorContext actorContext;

    /**
     * Creates query application service.
     */
    public SimulationQueryApplicationService(SimulationProjectionStorePort projectionStore, ActorContext actorContext) {
        this.projectionStore = projectionStore;
        this.actorContext = actorContext;
    }

    /**
     * Finds a run by run id or tracking id.
     */
    public Optional<SimulationRunSnapshot> findRun(String runOrTrackingId) {
        return projectionStore.findRun(actorContext.currentActorId(), runOrTrackingId);
    }

    /**
     * Lists timeline events by run id or tracking id.
     */
    public List<SimulationTimelineEventSnapshot> listTimeline(String runOrTrackingId) {
        return projectionStore.listTimeline(actorContext.currentActorId(), runOrTrackingId);
    }
}
