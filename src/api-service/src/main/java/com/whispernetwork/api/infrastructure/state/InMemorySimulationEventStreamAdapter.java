package com.whispernetwork.api.infrastructure.state;

import com.whispernetwork.api.application.dto.SimulationTimelineEventSnapshot;
import com.whispernetwork.api.application.ports.out.SimulationEventStreamPort;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * In-memory SSE emitter registry and dispatcher.
 */
@Component
public class InMemorySimulationEventStreamAdapter implements SimulationEventStreamPort {
    private final Map<String, CopyOnWriteArrayList<SseEmitter>> emittersByIdentifier;

    public InMemorySimulationEventStreamAdapter() {
        this.emittersByIdentifier = new ConcurrentHashMap<>();
    }

    @Override
    public SseEmitter open(String ownerId, String identifier, List<SimulationTimelineEventSnapshot> replayEvents) {
        String key = toKey(ownerId, identifier);
        SseEmitter emitter = new SseEmitter(0L);
        emittersByIdentifier
                .computeIfAbsent(key, ignored -> new CopyOnWriteArrayList<>())
                .add(emitter);

        emitter.onCompletion(() -> removeEmitter(key, emitter));
        emitter.onTimeout(() -> removeEmitter(key, emitter));
        emitter.onError(ignored -> removeEmitter(key, emitter));

        try {
            emitter.send(SseEmitter.event().name("CONNECTED").data(Map.of("id", identifier)));
            for (SimulationTimelineEventSnapshot event : replayEvents) {
                emitter.send(SseEmitter.event().name(event.eventType()).data(event));
            }
        } catch (Exception sendException) {
            removeEmitter(key, emitter);
            emitter.completeWithError(sendException);
        }

        return emitter;
    }

    @Override
    public void publish(String ownerId, String identifier, SimulationTimelineEventSnapshot event) {
        CopyOnWriteArrayList<SseEmitter> emitters = emittersByIdentifier.get(toKey(ownerId, identifier));
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(event.eventType()).data(event));
            } catch (Exception sendException) {
                emitters.remove(emitter);
                emitter.completeWithError(sendException);
            }
        }
    }

    private void removeEmitter(String key, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = emittersByIdentifier.get(key);
        if (emitters == null) {
            return;
        }

        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emittersByIdentifier.remove(key);
        }
    }

    private static String toKey(String ownerId, String identifier) {
        return ownerId + "::" + identifier;
    }
}
