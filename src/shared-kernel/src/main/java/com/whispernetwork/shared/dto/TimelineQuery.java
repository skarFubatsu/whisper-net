package com.whispernetwork.shared.dto;

import java.util.List;

/**
 * Filter model for timeline/history queries.
 */
public record TimelineQuery(String simulationRunId, String networkId, List<String> eventTypes, TimelineWindow window) {

    /**
     * Creates query with normalized optional fields.
     */
    public TimelineQuery {
        eventTypes = eventTypes == null ? List.of() : List.copyOf(eventTypes);
        window = window == null ? new TimelineWindow(null, null, 100, 0L) : window;
    }
}
