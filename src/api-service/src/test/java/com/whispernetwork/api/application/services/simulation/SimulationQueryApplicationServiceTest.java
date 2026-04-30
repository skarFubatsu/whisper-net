package com.whispernetwork.api.application.services.simulation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.whispernetwork.api.application.dto.SimulationRunSnapshot;
import com.whispernetwork.api.application.dto.SimulationTimelineEventSnapshot;
import com.whispernetwork.api.application.ports.out.SimulationProjectionStorePort;
import com.whispernetwork.api.application.security.ActorContext;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SimulationQueryApplicationServiceTest {

    @Mock
    private SimulationProjectionStorePort projectionStore;

    @Mock
    private ActorContext actorContext;

    private SimulationQueryApplicationService service;

    @BeforeEach
    void setUp() {
        service = new SimulationQueryApplicationService(projectionStore, actorContext);
        when(actorContext.currentActorId()).thenReturn("owner-1");
    }

    @Nested
    @DisplayName("findRun")
    class FindRunTests {

        @Test
        @DisplayName("returns run for current owner")
        void returnsRunForCurrentOwner() {
            SimulationRunSnapshot run = new SimulationRunSnapshot(
                    "run-1", "net-1", 3, "STARTED", 1, 12, null, Instant.EPOCH, Instant.EPOCH);
            when(projectionStore.findRun("owner-1", "run-1")).thenReturn(Optional.of(run));

            Optional<SimulationRunSnapshot> found = service.findRun("run-1");

            assertTrue(found.isPresent());
            assertEquals("run-1", found.get().runId());
            verify(projectionStore).findRun("owner-1", "run-1");
        }

        @Test
        @DisplayName("returns empty when run is not found")
        void returnsEmptyWhenRunMissing() {
            when(projectionStore.findRun("owner-1", "missing")).thenReturn(Optional.empty());

            Optional<SimulationRunSnapshot> found = service.findRun("missing");

            assertTrue(found.isEmpty());
            verify(projectionStore).findRun("owner-1", "missing");
        }
    }

    @Nested
    @DisplayName("listTimeline")
    class ListTimelineTests {

        @Test
        @DisplayName("returns timeline events for current owner")
        void returnsTimelineForCurrentOwner() {
            SimulationTimelineEventSnapshot event = new SimulationTimelineEventSnapshot(
                    "STARTED",
                    "run-1",
                    "net-1",
                    null,
                    null,
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
            when(projectionStore.listTimeline("owner-1", "run-1")).thenReturn(List.of(event));

            List<SimulationTimelineEventSnapshot> timeline = service.listTimeline("run-1");

            assertEquals(1, timeline.size());
            assertEquals("STARTED", timeline.get(0).eventType());
            verify(projectionStore).listTimeline("owner-1", "run-1");
        }
    }
}
