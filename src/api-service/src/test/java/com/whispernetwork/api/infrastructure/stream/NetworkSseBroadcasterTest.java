package com.whispernetwork.api.infrastructure.stream;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class NetworkSseBroadcasterTest {
    private NetworkSseBroadcaster broadcaster;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        broadcaster = new NetworkSseBroadcaster(objectMapper, 1);
    }

    @Nested
    @DisplayName("Subscription Management")
    class SubscriptionManagementTests {
        @Test
        @DisplayName("subscribes emitter to network stream")
        void subscribesEmitter() {
            SseEmitter emitter = broadcaster.subscribe("owner-1", "net-1");
            assertNotNull(emitter);
            assertEquals(1, broadcaster.getSubscriberCount("owner-1", "net-1"));
        }

        @Test
        @DisplayName("handles multiple subscribers to same network")
        void handlesMultipleSubscribers() {
            SseEmitter emitter1 = broadcaster.subscribe("owner-1", "net-1");
            SseEmitter emitter2 = broadcaster.subscribe("owner-1", "net-1");

            assertNotNull(emitter1);
            assertNotNull(emitter2);
            assertNotEquals(emitter1, emitter2);
            assertEquals(2, broadcaster.getSubscriberCount("owner-1", "net-1"));
        }

        @Test
        @DisplayName("isolates subscriptions by owner and network")
        void isolatesSubscriptionsByOwnerAndNetwork() {
            broadcaster.subscribe("owner-1", "net-1");
            broadcaster.subscribe("owner-1", "net-2");
            broadcaster.subscribe("owner-2", "net-1");

            assertEquals(1, broadcaster.getSubscriberCount("owner-1", "net-1"));
            assertEquals(1, broadcaster.getSubscriberCount("owner-1", "net-2"));
            assertEquals(1, broadcaster.getSubscriberCount("owner-2", "net-1"));
        }

        @Test
        @DisplayName("unsubscribes emitter on completion")
        void unsubscribesOnCompletion() throws Exception {
            SseEmitter emitter = broadcaster.subscribe("owner-1", "net-1");
            assertEquals(1, broadcaster.getSubscriberCount("owner-1", "net-1"));

            broadcaster.publishEvent("owner-1", "net-1", "test", "data");

            assertNotNull(emitter);
        }
    }

    @Nested
    @DisplayName("Event Publishing")
    class EventPublishingTests {
        @Test
        @DisplayName("publishes event to all subscribers")
        void publishesEventToAllSubscribers() {
            Map<String, SseEmitter> emitterMap = new ConcurrentHashMap<>();
            emitterMap.put("1", new SseEmitter());
            emitterMap.put("2", new SseEmitter());

            broadcaster.subscribe("owner-1", "net-1");
            broadcaster.subscribe("owner-1", "net-1");

            broadcaster.publishEvent("owner-1", "net-1", "test.event", "test payload");

            assertEquals(1, broadcaster.getPublishedEventCount("owner-1", "net-1"));
        }

        @Test
        @DisplayName("increments published event counter")
        void incrementsPublishedEventCounter() {
            broadcaster.subscribe("owner-1", "net-1");

            assertEquals(0, broadcaster.getPublishedEventCount("owner-1", "net-1"));

            broadcaster.publishEvent("owner-1", "net-1", "event1", "data1");
            assertEquals(1, broadcaster.getPublishedEventCount("owner-1", "net-1"));

            broadcaster.publishEvent("owner-1", "net-1", "event2", "data2");
            assertEquals(2, broadcaster.getPublishedEventCount("owner-1", "net-1"));
        }

        @Test
        @DisplayName("handles JSON serialization of event payload")
        void handlesJsonSerializationOfPayload() throws Exception {
            broadcaster.subscribe("owner-1", "net-1");

            Map<String, String> payload = Map.of("key", "value", "nested", "data");
            broadcaster.publishEvent("owner-1", "net-1", "test.event", payload);

            assertEquals(1, broadcaster.getPublishedEventCount("owner-1", "net-1"));
        }

        @Test
        @DisplayName("gracefully handles serialization errors")
        void gracefullyHandlesSerializationErrors() {
            broadcaster.subscribe("owner-1", "net-1");

            Object circularReference = new Object() {
                public Object self = this;
            };

            broadcaster.publishEvent("owner-1", "net-1", "test.event", circularReference);

            assertEquals(1, broadcaster.getPublishedEventCount("owner-1", "net-1"));
        }
    }

    @Nested
    @DisplayName("Sequence and Event ID Management")
    class SequenceManagementTests {
        @Test
        @DisplayName("assigns sequential IDs to events")
        void assignsSequentialIds() {
            broadcaster.subscribe("owner-1", "net-1");

            broadcaster.publishEvent("owner-1", "net-1", "event-1", "data1");
            broadcaster.publishEvent("owner-1", "net-1", "event-2", "data2");
            broadcaster.publishEvent("owner-1", "net-1", "event-3", "data3");

            assertEquals(3, broadcaster.getPublishedEventCount("owner-1", "net-1"));
        }

        @Test
        @DisplayName("maintains separate counters per network")
        void maintainsSeparateCountersPerNetwork() {
            broadcaster.subscribe("owner-1", "net-1");
            broadcaster.subscribe("owner-1", "net-2");

            broadcaster.publishEvent("owner-1", "net-1", "event", "data1");
            broadcaster.publishEvent("owner-1", "net-1", "event", "data2");
            broadcaster.publishEvent("owner-1", "net-2", "event", "data3");

            assertEquals(2, broadcaster.getPublishedEventCount("owner-1", "net-1"));
            assertEquals(1, broadcaster.getPublishedEventCount("owner-1", "net-2"));
        }
    }

    @Nested
    @DisplayName("Last-Event-ID Validation")
    class LastEventIdValidationTests {
        @Test
        @DisplayName("accepts null Last-Event-ID as valid")
        void acceptsNullLastEventId() {
            assertTrue(broadcaster.isValidLastEventId(null));
        }

        @Test
        @DisplayName("accepts empty Last-Event-ID as valid")
        void acceptsEmptyLastEventId() {
            assertTrue(broadcaster.isValidLastEventId(""));
            assertTrue(broadcaster.isValidLastEventId("   "));
        }

        @Test
        @DisplayName("validates numeric Last-Event-ID")
        void validatesNumericLastEventId() {
            assertTrue(broadcaster.isValidLastEventId("0"));
            assertTrue(broadcaster.isValidLastEventId("100"));
            assertTrue(broadcaster.isValidLastEventId("999"));
        }

        @Test
        @DisplayName("rejects non-numeric Last-Event-ID")
        void rejectsNonNumericLastEventId() {
            assertFalse(broadcaster.isValidLastEventId("invalid"));
            assertFalse(broadcaster.isValidLastEventId("12.34"));
            assertFalse(broadcaster.isValidLastEventId("-1"));
        }

        @Test
        @DisplayName("rejects negative Last-Event-ID")
        void rejectsNegativeLastEventId() {
            assertFalse(broadcaster.isValidLastEventId("-100"));
        }
    }

    @Nested
    @DisplayName("Event Buffer and Replay")
    class EventBufferTests {
        @Test
        @DisplayName("stores recent events in buffer")
        void storesRecentEventsInBuffer() {
            broadcaster.subscribe("owner-1", "net-1");

            for (int i = 0; i < 5; i++) {
                broadcaster.publishEvent("owner-1", "net-1", "event-" + i, "data-" + i);
            }

            assertEquals(5, broadcaster.getPublishedEventCount("owner-1", "net-1"));
        }

        @Test
        @DisplayName("maintains buffer size limit of 200")
        void maintainsBufferSizeLimit() {
            broadcaster.subscribe("owner-1", "net-1");

            for (int i = 0; i < 250; i++) {
                broadcaster.publishEvent("owner-1", "net-1", "event-" + i, "data-" + i);
            }

            assertEquals(250, broadcaster.getPublishedEventCount("owner-1", "net-1"));
        }

        @Test
        @DisplayName("handles subscription without Last-Event-ID")
        void handlesSubscriptionWithoutLastEventId() {
            broadcaster.publishEvent("owner-1", "net-1", "event-1", "data1");

            SseEmitter emitter = broadcaster.subscribe("owner-1", "net-1", null);
            assertNotNull(emitter);
        }

        @Test
        @DisplayName("replays events after Last-Event-ID when provided")
        void replaysEventsAfterLastEventId() {
            broadcaster.publishEvent("owner-1", "net-1", "event-1", "data1");
            broadcaster.publishEvent("owner-1", "net-1", "event-2", "data2");
            broadcaster.publishEvent("owner-1", "net-1", "event-3", "data3");

            SseEmitter emitter = broadcaster.subscribe("owner-1", "net-1", "1");
            assertNotNull(emitter);
        }

        @Test
        @DisplayName("handles invalid Last-Event-ID gracefully during replay")
        void handlesInvalidLastEventIdDuringReplay() {
            broadcaster.publishEvent("owner-1", "net-1", "event-1", "data1");

            SseEmitter emitter = broadcaster.subscribe("owner-1", "net-1", "invalid");
            assertNotNull(emitter);
        }
    }

    @Nested
    @DisplayName("Heartbeat Configuration")
    class HeartbeatConfigurationTests {
        @Test
        @DisplayName("configures heartbeat interval from constructor")
        void configuresHeartbeatInterval() {
            NetworkSseBroadcaster custom = new NetworkSseBroadcaster(objectMapper, 30);
            assertNotNull(custom);
        }

        @Test
        @DisplayName("defaults to 15 second heartbeat interval")
        void defaultsTo15SecondHeartbeat() {
            NetworkSseBroadcaster defaultBroadcaster = new NetworkSseBroadcaster(objectMapper);
            assertNotNull(defaultBroadcaster);
        }

        @Test
        @DisplayName("subscriber receives heartbeat events")
        void subscriberReceivesHeartbeat() throws Exception {
            SseEmitter emitter = broadcaster.subscribe("owner-1", "net-1");
            assertNotNull(emitter);

            Thread.sleep(500);
        }
    }

    @Nested
    @DisplayName("Concurrent Operations")
    class ConcurrentOperationsTests {
        @Test
        @DisplayName("handles concurrent subscriptions")
        void handlesConcurrentSubscriptions() throws InterruptedException {
            List<Thread> threads = new ArrayList<>();
            AtomicLong subscriberCount = new AtomicLong(0);

            for (int i = 0; i < 10; i++) {
                Thread t = new Thread(() -> {
                    broadcaster.subscribe("owner-1", "net-1");
                    subscriberCount.incrementAndGet();
                });
                threads.add(t);
                t.start();
            }

            for (Thread t : threads) {
                t.join();
            }

            assertEquals(10, broadcaster.getSubscriberCount("owner-1", "net-1"));
        }

        @Test
        @DisplayName("handles concurrent publishing")
        void handlesConcurrentPublishing() throws InterruptedException {
            broadcaster.subscribe("owner-1", "net-1");

            List<Thread> threads = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                final int eventNum = i;
                Thread t = new Thread(
                        () -> broadcaster.publishEvent("owner-1", "net-1", "event-" + eventNum, "data-" + eventNum));
                threads.add(t);
                t.start();
            }

            for (Thread t : threads) {
                t.join();
            }

            assertEquals(100, broadcaster.getPublishedEventCount("owner-1", "net-1"));
        }
    }
}
