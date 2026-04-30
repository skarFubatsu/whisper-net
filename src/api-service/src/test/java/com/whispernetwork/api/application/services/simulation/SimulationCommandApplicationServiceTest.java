package com.whispernetwork.api.application.services.simulation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.whispernetwork.api.application.dto.CancelSimulationCommand;
import com.whispernetwork.api.application.dto.SimulationRunSnapshot;
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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SimulationCommandApplicationServiceTest {

    @Mock
    private SimulationCommandPublisherPort commandPublisher;

    @Mock
    private SimulationProjectionStorePort projectionStore;

    @Mock
    private StartAcknowledgementPort startAcknowledgement;

    @Mock
    private NetworkVersionQueryPort networkVersionQuery;

    @Mock
    private ActorContext actorContext;

    private SimulationCommandApplicationService service;

    @BeforeEach
    void setUp() {
        service = new SimulationCommandApplicationService(
                commandPublisher, projectionStore, startAcknowledgement, networkVersionQuery, actorContext);
    }

    @Nested
    @DisplayName("startSimulation")
    class StartSimulationTests {

        @Test
        @DisplayName("returns run id when start acknowledgement completes")
        void returnsRunIdWhenAcknowledged() {
            StartSimulationCommand command = startCommand("owner-1", "owner-1", "track-1");
            when(actorContext.currentActorId()).thenReturn("owner-1");
            when(networkVersionQuery.exists("owner-1", "net-1", 3)).thenReturn(true);
            when(startAcknowledgement.createPending("track-1")).thenReturn(CompletableFuture.completedFuture("run-1"));

            String result = service.startSimulation(command);

            assertEquals("run-1", result);
            verify(projectionStore).upsertRequested("owner-1", "track-1", "net-1", 3, 12);
            verify(commandPublisher).publishStart(command);
            verify(startAcknowledgement).clear("track-1");

            ArgumentCaptor<SimulationTimelineEventSnapshot> eventCaptor =
                    ArgumentCaptor.forClass(SimulationTimelineEventSnapshot.class);
            verify(projectionStore).appendTimelineForTracking(eq("owner-1"), eq("track-1"), eventCaptor.capture());
            assertEquals("REQUESTED", eventCaptor.getValue().eventType());
            assertEquals("net-1", eventCaptor.getValue().networkId());
        }

        @Test
        @DisplayName("returns tracking id when start acknowledgement times out")
        void returnsTrackingIdWhenAckTimesOut() {
            StartSimulationCommand command = startCommand("owner-1", "owner-1", "track-timeout");
            when(actorContext.currentActorId()).thenReturn("owner-1");
            when(networkVersionQuery.exists("owner-1", "net-1", 3)).thenReturn(true);
            when(startAcknowledgement.createPending("track-timeout")).thenReturn(new CompletableFuture<>());

            String result = service.startSimulation(command);

            assertEquals("track-timeout", result);
            verify(commandPublisher).publishStart(command);
            verify(startAcknowledgement).clear("track-timeout");
        }

        @Test
        @DisplayName("throws not found when requested network version does not exist")
        void throwsWhenVersionMissing() {
            StartSimulationCommand command = startCommand("owner-1", "owner-1", "track-1");
            when(actorContext.currentActorId()).thenReturn("owner-1");
            when(networkVersionQuery.exists("owner-1", "net-1", 3)).thenReturn(false);

            assertThrows(NotFoundException.class, () -> service.startSimulation(command));
            verify(commandPublisher, never()).publishStart(any());
            verify(projectionStore, never()).upsertRequested(anyString(), anyString(), anyString(), anyInt(), anyInt());
        }

        @Test
        @DisplayName("throws forbidden when owner does not match current actor")
        void throwsWhenOwnerMismatch() {
            StartSimulationCommand command = startCommand("owner-1", "owner-1", "track-1");
            when(actorContext.currentActorId()).thenReturn("owner-2");

            assertThrows(ForbiddenException.class, () -> service.startSimulation(command));
            verifyNoInteractions(networkVersionQuery);
        }

        @Test
        @DisplayName("throws forbidden when actor does not match owner")
        void throwsWhenActorMismatch() {
            StartSimulationCommand command = startCommand("owner-1", "owner-2", "track-1");
            when(actorContext.currentActorId()).thenReturn("owner-1");

            assertThrows(ForbiddenException.class, () -> service.startSimulation(command));
            verifyNoInteractions(networkVersionQuery);
        }
    }

    @Nested
    @DisplayName("cancelSimulation")
    class CancelSimulationTests {

        @Test
        @DisplayName("returns false when run id cannot be resolved")
        void returnsFalseWhenRunNotResolved() {
            CancelSimulationCommand command = new CancelSimulationCommand("owner-1", "owner-1", "track-1");
            when(actorContext.currentActorId()).thenReturn("owner-1");
            when(projectionStore.resolveRunId("owner-1", "track-1")).thenReturn(Optional.empty());

            boolean cancelled = service.cancelSimulation("track-1", command);

            assertFalse(cancelled);
            verify(commandPublisher, never()).publishCancel(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("returns false when run projection is missing")
        void returnsFalseWhenRunProjectionMissing() {
            CancelSimulationCommand command = new CancelSimulationCommand("owner-1", "owner-1", "track-1");
            when(actorContext.currentActorId()).thenReturn("owner-1");
            when(projectionStore.resolveRunId("owner-1", "track-1")).thenReturn(Optional.of("run-1"));
            when(projectionStore.findRun("owner-1", "run-1")).thenReturn(Optional.empty());

            boolean cancelled = service.cancelSimulation("track-1", command);

            assertFalse(cancelled);
            verify(commandPublisher, never()).publishCancel(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("publishes cancel command and returns true when run exists")
        void publishesCancelWhenRunExists() {
            CancelSimulationCommand command = new CancelSimulationCommand("owner-1", "owner-1", "track-1");
            when(actorContext.currentActorId()).thenReturn("owner-1");
            when(projectionStore.resolveRunId("owner-1", "track-1")).thenReturn(Optional.of("run-1"));
            when(projectionStore.findRun("owner-1", "run-1"))
                    .thenReturn(Optional.of(new SimulationRunSnapshot(
                            "run-1", "net-1", 3, "STARTED", 0, 12, null, Instant.EPOCH, Instant.EPOCH)));

            boolean cancelled = service.cancelSimulation("track-1", command);

            assertTrue(cancelled);
            verify(commandPublisher).publishCancel("run-1", "net-1", command);
        }

        @Test
        @DisplayName("throws forbidden when owner does not match current actor")
        void throwsWhenOwnerMismatch() {
            CancelSimulationCommand command = new CancelSimulationCommand("owner-1", "owner-1", "track-1");
            when(actorContext.currentActorId()).thenReturn("owner-2");

            assertThrows(ForbiddenException.class, () -> service.cancelSimulation("track-1", command));
            verifyNoInteractions(projectionStore);
        }
    }

    private StartSimulationCommand startCommand(String ownerId, String actorId, String trackingId) {
        return new StartSimulationCommand("net-1", 3, ownerId, actorId, trackingId, 12);
    }
}
