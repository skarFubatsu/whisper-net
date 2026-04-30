package com.whispernetwork.api.application.services.simulation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.whispernetwork.api.application.dto.SimulationTimelineEventSnapshot;
import com.whispernetwork.api.application.ports.out.SimulationEventStreamPort;
import com.whispernetwork.api.application.ports.out.SimulationProjectionStorePort;
import com.whispernetwork.api.application.ports.out.StartAcknowledgementPort;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SimulationEventProjectionApplicationServiceTest {

    @Mock
    private SimulationProjectionStorePort projectionStore;

    @Mock
    private SimulationEventStreamPort eventStream;

    @Mock
    private StartAcknowledgementPort startAcknowledgement;

    private SimulationEventProjectionApplicationService service;

    @BeforeEach
    void setUp() {
        service = new SimulationEventProjectionApplicationService(projectionStore, eventStream, startAcknowledgement);
    }

    @Nested
    @DisplayName("onStarted")
    class OnStartedTests {

        @Test
        @DisplayName("projects started event, appends timeline, publishes to run and tracking, and completes ack")
        void projectsAndBroadcastsStarted() {
            Instant startedAt = Instant.now();
            when(projectionStore.resolveOwnerIdByTrackingId("track-1")).thenReturn(Optional.of("owner-1"));

            service.onStarted("track-1", "run-1", "net-1", 3, startedAt);

            verify(projectionStore).upsertStarted("owner-1", "track-1", "run-1", "net-1", 3, startedAt);
            verify(projectionStore)
                    .appendTimelineForRun(eq("owner-1"), eq("run-1"), any(SimulationTimelineEventSnapshot.class));
            verify(eventStream).publish(eq("owner-1"), eq("run-1"), any(SimulationTimelineEventSnapshot.class));
            verify(eventStream).publish(eq("owner-1"), eq("track-1"), any(SimulationTimelineEventSnapshot.class));
            verify(startAcknowledgement).completeIfPending("track-1", "run-1");

            ArgumentCaptor<SimulationTimelineEventSnapshot> eventCaptor =
                    ArgumentCaptor.forClass(SimulationTimelineEventSnapshot.class);
            verify(projectionStore).appendTimelineForRun(eq("owner-1"), eq("run-1"), eventCaptor.capture());
            assertEquals("STARTED", eventCaptor.getValue().eventType());
            assertEquals("run-1", eventCaptor.getValue().simulationRunId());
        }

        @Test
        @DisplayName("does nothing when owner cannot be resolved by tracking id")
        void noOpWhenOwnerMissing() {
            when(projectionStore.resolveOwnerIdByTrackingId("missing")).thenReturn(Optional.empty());

            service.onStarted("missing", "run-1", "net-1", 3, Instant.now());

            verifyNoMoreInteractions(eventStream, startAcknowledgement);
            verify(projectionStore, never())
                    .upsertStarted(anyString(), anyString(), anyString(), anyString(), anyInt(), any());
        }
    }

    @Nested
    @DisplayName("onCancelled")
    class OnCancelledTests {

        @Test
        @DisplayName("projects cancelled event and publishes to run and tracking")
        void projectsAndBroadcastsCancelled() {
            Instant cancelledAt = Instant.now();
            when(projectionStore.resolveOwnerIdByRunId("run-1")).thenReturn(Optional.of("owner-1"));
            when(projectionStore.resolveTrackingId("owner-1", "run-1")).thenReturn(Optional.of("track-1"));

            service.onCancelled("run-1", "net-1", cancelledAt);

            verify(projectionStore).upsertCancelled("owner-1", "run-1", "net-1", cancelledAt);
            verify(eventStream).publish(eq("owner-1"), eq("run-1"), any(SimulationTimelineEventSnapshot.class));
            verify(eventStream).publish(eq("owner-1"), eq("track-1"), any(SimulationTimelineEventSnapshot.class));

            ArgumentCaptor<SimulationTimelineEventSnapshot> eventCaptor =
                    ArgumentCaptor.forClass(SimulationTimelineEventSnapshot.class);
            verify(projectionStore).appendTimelineForRun(eq("owner-1"), eq("run-1"), eventCaptor.capture());
            assertEquals("CANCELLED", eventCaptor.getValue().eventType());
        }
    }

    @Nested
    @DisplayName("onOpinionUpdated")
    class OnOpinionUpdatedTests {

        @Test
        @DisplayName("appends opinion event and avoids duplicate publish when tracking equals run")
        void appendsOpinionAndAvoidsDuplicatePublish() {
            Instant occurredAt = Instant.now();
            when(projectionStore.resolveOwnerIdByRunId("run-1")).thenReturn(Optional.of("owner-1"));
            when(projectionStore.resolveTrackingId("owner-1", "run-1")).thenReturn(Optional.of("run-1"));

            service.onOpinionUpdated(
                    "run-1",
                    "net-1",
                    7,
                    "agent-1",
                    0.1,
                    0.7,
                    0.6,
                    List.of("agent-2", "agent-3"),
                    true,
                    "agent-2",
                    false,
                    occurredAt);

            verify(eventStream, times(1))
                    .publish(eq("owner-1"), eq("run-1"), any(SimulationTimelineEventSnapshot.class));

            ArgumentCaptor<SimulationTimelineEventSnapshot> eventCaptor =
                    ArgumentCaptor.forClass(SimulationTimelineEventSnapshot.class);
            verify(projectionStore).appendTimelineForRun(eq("owner-1"), eq("run-1"), eventCaptor.capture());

            SimulationTimelineEventSnapshot event = eventCaptor.getValue();
            assertEquals("OPINION_UPDATED", event.eventType());
            assertEquals(7, event.tickNumber());
            assertEquals("agent-1", event.agentId());
            assertEquals(2, event.contributingSourceAgentIds().size());
        }
    }

    @Nested
    @DisplayName("owner resolution")
    class OwnerResolutionTests {

        @Test
        @DisplayName("does nothing on failed event when owner cannot be resolved")
        void noOpWhenOwnerMissingForFailedEvent() {
            when(projectionStore.resolveOwnerIdByRunId("run-404")).thenReturn(Optional.empty());

            service.onFailed("run-404", "net-1", "boom", Instant.now());

            verify(projectionStore, never()).upsertFailed(anyString(), anyString(), anyString(), anyString(), any());
            verifyNoInteractions(eventStream);
        }
    }
}
