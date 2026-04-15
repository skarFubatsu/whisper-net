package com.whispernetwork.simulation.application.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.whispernetwork.shared.dto.RunStatus;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SimulationRun")
class SimulationRunTest {

    @Nested
    @DisplayName("Lifecycle")
    class LifecycleTests {

        @Test
        void shouldTransitionFromRequestedToCompletedThroughRunning() {
            SimulationRun run = new SimulationRun(UUID.randomUUID().toString(), "network-a", 1, "actor-a", "req-a", 2);

            run.markRunning();
            run.incrementCompletedTicks();
            run.incrementCompletedTicks();
            run.markCompleted();

            assertEquals(RunStatus.COMPLETED, run.getStatus());
            assertEquals(2, run.getCompletedTicks());
        }
    }

    @Nested
    @DisplayName("InvalidTransitions")
    class InvalidTransitionsTests {

        @Test
        void shouldRejectInvalidTransitionToCompletedFromRequested() {
            SimulationRun run = new SimulationRun(UUID.randomUUID().toString(), "network-a", 1, "actor-a", "req-a", 1);

            assertThrows(IllegalStateException.class, run::markCompleted);
        }
    }

    @Nested
    @DisplayName("Cancellation")
    class CancellationTests {

        @Test
        void shouldMarkCancellingFromRunningAndCaptureRequester() {
            SimulationRun run = new SimulationRun(UUID.randomUUID().toString(), "network-a", 1, "actor-a", "req-a", 1);
            run.markRunning();

            run.markCancelling("actor-b", "cancel-1");

            assertEquals(RunStatus.CANCELLING, run.getStatus());
            assertEquals("actor-b", run.getCancellationRequestedByActorId());
            assertEquals("cancel-1", run.getCancellationClientRequestId());
        }
    }
}
