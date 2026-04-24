package com.whispernetwork.api.infrastructure.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class NetworkSseBroadcaster {
    private final ObjectMapper objectMapper;
    private final Map<String, List<SseEmitter>> subscribers = new ConcurrentHashMap<>();

    // per-network sequence counter and recent event buffer for resume
    private final Map<String, AtomicLong> seqCounters = new ConcurrentHashMap<>();
    private final Map<String, Deque<StoredEvent>> recentEvents = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final int heartbeatSeconds;
    private static final int RECENT_BUFFER_SIZE = 200;
    private final Map<String, AtomicLong> publishedCounters = new ConcurrentHashMap<>();

    public NetworkSseBroadcaster() {
        this(new ObjectMapper(), 15);
    }

    public NetworkSseBroadcaster(
            ObjectMapper objectMapper, @Value("${sse.heartbeat.seconds:15}") int heartbeatSeconds) {
        this.objectMapper = objectMapper;
        this.heartbeatSeconds = heartbeatSeconds;
    }

    // convenience ctor for tests and programmatic uses
    public NetworkSseBroadcaster(ObjectMapper objectMapper) {
        this(objectMapper, 15);
    }

    public SseEmitter subscribe(String owner, String networkId) {
        return subscribe(owner, networkId, null);
    }

    public SseEmitter subscribe(String owner, String networkId, String lastEventId) {
        String key = key(owner, networkId);
        SseEmitter emitter = new SseEmitter(0L);
        subscribers.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> removeEmitter(key, emitter));
        emitter.onTimeout(() -> removeEmitter(key, emitter));
        emitter.onError(e -> removeEmitter(key, emitter));

        // replay recent events if client provided Last-Event-ID
        if (lastEventId != null && !lastEventId.isBlank()) {
            try {
                long last = Long.parseLong(lastEventId);
                Deque<StoredEvent> buf = recentEvents.getOrDefault(key, new ArrayDeque<>());
                for (StoredEvent se : buf) {
                    if (se.id > last) {
                        emitter.send(SseEmitter.event()
                                .id(Long.toString(se.id))
                                .name(se.type)
                                .data(se.data));
                    }
                }
            } catch (NumberFormatException ignored) {
            } catch (Exception ignored) {
            }
        }
        scheduler.scheduleAtFixedRate(
                () -> {
                    try {
                        emitter.send(SseEmitter.event()
                                .name("heartbeat")
                                .data(Instant.now().toString()));
                    } catch (Exception ignored) {
                        removeEmitter(key, emitter);
                    }
                },
                heartbeatSeconds,
                heartbeatSeconds,
                TimeUnit.SECONDS);
        return emitter;
    }

    public boolean isValidLastEventId(String lastEventId) {
        if (lastEventId == null || lastEventId.isBlank()) {
            return true;
        }
        try {
            return Long.parseLong(lastEventId) >= 0;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private void removeEmitter(String key, SseEmitter emitter) {
        List<SseEmitter> list = subscribers.get(key);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                subscribers.remove(key);
            }
        }
    }

    public void publishEvent(String owner, String networkId, String type, Object payload) {
        String key = key(owner, networkId);
        List<SseEmitter> list = subscribers.getOrDefault(key, List.of());

        AtomicLong counter = seqCounters.computeIfAbsent(key, k -> new AtomicLong(0L));
        Deque<StoredEvent> buf = recentEvents.computeIfAbsent(key, k -> new ArrayDeque<>());

        long id = counter.incrementAndGet();
        String data;
        try {
            data = objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            data = "{}";
        }

        synchronized (buf) {
            buf.addLast(new StoredEvent(id, type, data));
            while (buf.size() > RECENT_BUFFER_SIZE) buf.removeFirst();
        }

        publishedCounters.computeIfAbsent(key, k -> new AtomicLong(0L)).incrementAndGet();

        if (list.isEmpty()) {
            return;
        }

        for (Iterator<SseEmitter> it = list.iterator(); it.hasNext(); ) {
            SseEmitter emitter = it.next();
            try {
                SseEmitter.SseEventBuilder event =
                        SseEmitter.event().id(Long.toString(id)).name(type).data(data);
                emitter.send(event);
            } catch (Exception ex) {
                removeEmitter(key, emitter);
            }
        }
    }

    public int getSubscriberCount(String owner, String networkId) {
        return subscribers.getOrDefault(key(owner, networkId), List.of()).size();
    }

    public long getPublishedEventCount(String owner, String networkId) {
        return publishedCounters
                .getOrDefault(key(owner, networkId), new AtomicLong(0L))
                .get();
    }

    private String key(String owner, String networkId) {
        return owner + ":" + networkId;
    }

    private static final class StoredEvent {
        final long id;
        final String type;
        final String data;

        StoredEvent(long id, String type, String data) {
            this.id = id;
            this.type = type;
            this.data = data;
        }
    }
}
