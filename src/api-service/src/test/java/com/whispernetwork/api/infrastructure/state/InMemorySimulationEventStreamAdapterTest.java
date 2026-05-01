package com.whispernetwork.api.infrastructure.state;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.whispernetwork.api.application.dto.SimulationTimelineEventSnapshot;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class InMemorySimulationEventStreamAdapterTest {

    @Nested
    @DisplayName("open")
    class OpenTests {

        @Test
        @DisplayName("registers emitter for owner and identifier")
        void registersEmitter() throws Exception {
            InMemorySimulationEventStreamAdapter adapter = new InMemorySimulationEventStreamAdapter();

            SseEmitter emitter = adapter.open("owner-1", "run-1", List.of());

            assertEquals(1, emitterCount(adapter, "owner-1", "run-1"));
            emitter.complete();
        }

        @Test
        @DisplayName("replaces key space by owner and identifier")
        void ownerIsolationByKey() throws Exception {
            InMemorySimulationEventStreamAdapter adapter = new InMemorySimulationEventStreamAdapter();

            adapter.open("owner-1", "run-1", List.of());
            adapter.open("owner-2", "run-1", List.of());

            assertEquals(1, emitterCount(adapter, "owner-1", "run-1"));
            assertEquals(1, emitterCount(adapter, "owner-2", "run-1"));
        }
    }

    @Nested
    @DisplayName("publish")
    class PublishTests {

        @Test
        @DisplayName("is no-op when no emitters are registered")
        void noOpWithoutEmitters() {
            InMemorySimulationEventStreamAdapter adapter = new InMemorySimulationEventStreamAdapter();

            assertDoesNotThrow(() -> adapter.publish("owner-1", "missing", sampleEvent("STARTED")));
        }

        @Test
        @DisplayName("removes failing emitter when send throws")
        void removesFailingEmitterOnSendError() throws Exception {
            InMemorySimulationEventStreamAdapter adapter = new InMemorySimulationEventStreamAdapter();
            String key = "owner-1::run-1";

            SseEmitter failingEmitter = new SseEmitter(0L) {
                @Override
                public synchronized void send(SseEventBuilder builder) {
                    throw new RuntimeException("send failed");
                }
            };

            emittersByKey(adapter).put(key, new CopyOnWriteArrayList<>(List.of(failingEmitter)));

            adapter.publish("owner-1", "run-1", sampleEvent("TICK_COMPLETED"));

            assertEquals(0, emitterCount(adapter, "owner-1", "run-1"));
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, CopyOnWriteArrayList<SseEmitter>> emittersByKey(InMemorySimulationEventStreamAdapter adapter)
            throws Exception {
        Field field = InMemorySimulationEventStreamAdapter.class.getDeclaredField("emittersByIdentifier");
        field.setAccessible(true);
        return (Map<String, CopyOnWriteArrayList<SseEmitter>>) field.get(adapter);
    }

    private int emitterCount(InMemorySimulationEventStreamAdapter adapter, String ownerId, String identifier)
            throws Exception {
        return emittersByKey(adapter)
                .getOrDefault(ownerId + "::" + identifier, new CopyOnWriteArrayList<>())
                .size();
    }

    private SimulationTimelineEventSnapshot sampleEvent(String type) {
        return new SimulationTimelineEventSnapshot(
                type,
                "run-1",
                "net-1",
                1,
                1,
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
                Instant.EPOCH);
    }
}
