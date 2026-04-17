package com.whispernetwork.api.application.ports.out;

import com.whispernetwork.api.application.dto.SimulationTimelineEventSnapshot;
import java.util.List;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Manages live event stream subscriptions and dispatching.
 */
public interface SimulationEventStreamPort {

    /**
     * Opens an SSE stream and replays existing events.
     */
    SseEmitter open(String ownerId, String identifier, List<SimulationTimelineEventSnapshot> replayEvents);

    /**
     * Publishes one timeline event to all subscribers of the given identifier.
     */
    void publish(String ownerId, String identifier, SimulationTimelineEventSnapshot event);
}
