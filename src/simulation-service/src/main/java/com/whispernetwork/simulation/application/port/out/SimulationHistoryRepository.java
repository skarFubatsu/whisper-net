package com.whispernetwork.simulation.application.port.out;

import com.whispernetwork.shared.dto.SimulationHistoryEntry;
import com.whispernetwork.shared.dto.TimelineQuery;
import com.whispernetwork.simulation.application.model.SimulationEvent;
import java.util.List;

/**
 * Persistence boundary for simulation timeline and opinion update history.
 */
public interface SimulationHistoryRepository {

    /**
     * Persists one simulation event and any derived details.
     */
    void record(SimulationEvent event);

    /**
     * Lists timeline rows according to the provided filter and paging window.
     */
    List<SimulationHistoryEntry> list(TimelineQuery query);
}
