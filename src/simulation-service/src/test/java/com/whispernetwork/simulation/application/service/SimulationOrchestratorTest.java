package com.whispernetwork.simulation.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.whispernetwork.shared.dto.RunStatus;
import com.whispernetwork.simulation.application.model.IdempotencyKey;
import com.whispernetwork.simulation.application.model.SimulationCancelCommand;
import com.whispernetwork.simulation.application.model.SimulationEvent;
import com.whispernetwork.simulation.application.model.SimulationRun;
import com.whispernetwork.simulation.application.model.SimulationStartCommand;
import com.whispernetwork.simulation.application.port.out.SimulationEventPort;
import com.whispernetwork.simulation.application.port.out.SimulationRunRepository;
import com.whispernetwork.simulation.core.engine.OpinionAggregator;
import com.whispernetwork.simulation.core.engine.SimulationTickEngine;
import com.whispernetwork.simulation.infrastructure.inmemory.InMemoryInfluenceNetworkProvider;
import com.whispernetwork.simulation.infrastructure.inmemory.RuntimeNetworkFixtures;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class SimulationOrchestratorTest {

    @Test
    void shouldRunSimulationAndPublishLifecycleEvents() {
        StubSimulationRunRepository repository = new StubSimulationRunRepository();
        CapturingSimulationEventPort eventPort = new CapturingSimulationEventPort();
        InMemoryInfluenceNetworkProvider networkProvider = new InMemoryInfluenceNetworkProvider();
        RuntimeNetworkFixtures.registerDefaults(networkProvider);

        try (ExecutorService executor = new DirectExecutorService();
                SimulationOrchestrator orchestrator = new SimulationOrchestrator(
                        new SimulationTickEngine(new OpinionAggregator()),
                        repository,
                        networkProvider,
                        eventPort,
                        executor)) {

            String runId = orchestrator.requestSimulation(
                    new SimulationStartCommand("golden-cascade", 1, "actor-1", "req-1", 2));

            SimulationRun run = repository.findById(runId).orElseThrow();
            assertEquals(RunStatus.COMPLETED, run.getStatus());
            assertEquals(2, run.getCompletedTicks());

            assertEquals(4, eventPort.events.size());
            assertInstanceOf(SimulationEvent.SimulationStarted.class, eventPort.events.get(0));
            assertInstanceOf(SimulationEvent.SimulationTickCompleted.class, eventPort.events.get(1));
            assertInstanceOf(SimulationEvent.SimulationTickCompleted.class, eventPort.events.get(2));
            assertInstanceOf(SimulationEvent.SimulationCompleted.class, eventPort.events.get(3));
        }
    }

    @Test
    void shouldReturnExistingRunForDuplicateStartCommand() {
        StubSimulationRunRepository repository = new StubSimulationRunRepository();
        CapturingSimulationEventPort eventPort = new CapturingSimulationEventPort();
        InMemoryInfluenceNetworkProvider networkProvider = new InMemoryInfluenceNetworkProvider();
        RuntimeNetworkFixtures.registerDefaults(networkProvider);

        try (ExecutorService executor = new DirectExecutorService();
                SimulationOrchestrator orchestrator = new SimulationOrchestrator(
                        new SimulationTickEngine(new OpinionAggregator()),
                        repository,
                        networkProvider,
                        eventPort,
                        executor)) {

            SimulationStartCommand command =
                    new SimulationStartCommand("golden-cascade", 1, "actor-1", "req-idempotent", 1);

            String firstRunId = orchestrator.requestSimulation(command);
            String secondRunId = orchestrator.requestSimulation(command);

            assertEquals(firstRunId, secondRunId);
            assertEquals(3, eventPort.events.size());
        }
    }

    @Test
    void shouldRejectNewStartWhenActiveRunExistsForSameNetwork() {
        StubSimulationRunRepository repository = new StubSimulationRunRepository();
        SimulationRun activeRun =
                new SimulationRun(UUID.randomUUID().toString(), "network-a", 1, "actor-1", "req-1", 1);
        activeRun.markRunning();
        repository.save(activeRun);

        InMemoryInfluenceNetworkProvider networkProvider = new InMemoryInfluenceNetworkProvider();
        RuntimeNetworkFixtures.registerDefaults(networkProvider);

        try (ExecutorService executor = new DirectExecutorService();
                SimulationOrchestrator orchestrator = new SimulationOrchestrator(
                        new SimulationTickEngine(new OpinionAggregator()),
                        repository,
                        networkProvider,
                        eventPortNoop(),
                        executor)) {

            assertThrows(
                    IllegalStateException.class,
                    () -> orchestrator.requestSimulation(
                            new SimulationStartCommand("network-a", 1, "actor-2", "req-2", 1)));
        }
    }

    @Test
    void shouldCancelExistingRunWhenNetworkMatches() {
        StubSimulationRunRepository repository = new StubSimulationRunRepository();
        SimulationRun run = new SimulationRun(UUID.randomUUID().toString(), "network-a", 1, "actor-1", "req-1", 1);
        repository.save(run);

        InMemoryInfluenceNetworkProvider networkProvider = new InMemoryInfluenceNetworkProvider();
        RuntimeNetworkFixtures.registerDefaults(networkProvider);

        try (ExecutorService executor = new DirectExecutorService();
                SimulationOrchestrator orchestrator = new SimulationOrchestrator(
                        new SimulationTickEngine(new OpinionAggregator()),
                        repository,
                        networkProvider,
                        eventPortNoop(),
                        executor)) {

            boolean cancelled = orchestrator.requestCancellation(
                    new SimulationCancelCommand(run.getId(), "network-a", "actor-2", "cancel-1"));

            assertTrue(cancelled);
            SimulationRun updated = repository.findById(run.getId()).orElseThrow();
            assertEquals(RunStatus.CANCELLING, updated.getStatus());
            assertEquals("actor-2", updated.getCancellationRequestedByActorId());
            assertEquals("cancel-1", updated.getCancellationClientRequestId());
        }
    }

    @Test
    void shouldReturnFalseWhenCancelNetworkDoesNotMatch() {
        StubSimulationRunRepository repository = new StubSimulationRunRepository();
        SimulationRun run = new SimulationRun(UUID.randomUUID().toString(), "network-a", 1, "actor-1", "req-1", 1);
        repository.save(run);

        InMemoryInfluenceNetworkProvider networkProvider = new InMemoryInfluenceNetworkProvider();
        RuntimeNetworkFixtures.registerDefaults(networkProvider);

        try (ExecutorService executor = new DirectExecutorService();
                SimulationOrchestrator orchestrator = new SimulationOrchestrator(
                        new SimulationTickEngine(new OpinionAggregator()),
                        repository,
                        networkProvider,
                        eventPortNoop(),
                        executor)) {

            boolean cancelled = orchestrator.requestCancellation(
                    new SimulationCancelCommand(run.getId(), "network-b", "actor-2", "cancel-1"));

            assertFalse(cancelled);
            assertEquals(
                    RunStatus.REQUESTED,
                    repository.findById(run.getId()).orElseThrow().getStatus());
        }
    }

    private static SimulationEventPort eventPortNoop() {
        return ignored -> {};
    }

    private static final class CapturingSimulationEventPort implements SimulationEventPort {
        private final List<SimulationEvent> events = new ArrayList<>();

        @Override
        public void publish(SimulationEvent event) {
            events.add(event);
        }
    }

    private static final class StubSimulationRunRepository implements SimulationRunRepository {
        private final Map<String, SimulationRun> runsById = new ConcurrentHashMap<>();
        private final Map<IdempotencyKey, String> runByIdempotency = new ConcurrentHashMap<>();

        @Override
        public void save(SimulationRun run) {
            runsById.put(run.getId(), run);
        }

        @Override
        public Optional<SimulationRun> findById(String runId) {
            return Optional.ofNullable(runsById.get(runId));
        }

        @Override
        public Optional<SimulationRun> findActiveByNetwork(String networkId) {
            return runsById.values().stream()
                    .filter(run -> run.getNetworkId().equals(networkId))
                    .filter(run -> run.getStatus() == RunStatus.REQUESTED
                            || run.getStatus() == RunStatus.RUNNING
                            || run.getStatus() == RunStatus.CANCELLING)
                    .findFirst();
        }

        @Override
        public void putIdempotency(IdempotencyKey key, String runId) {
            runByIdempotency.putIfAbsent(key, runId);
        }

        @Override
        public Optional<String> findByIdempotency(IdempotencyKey key) {
            return Optional.ofNullable(runByIdempotency.get(key));
        }
    }

    private static final class DirectExecutorService extends AbstractExecutorService {
        private volatile boolean shutdown;

        @Override
        public void shutdown() {
            shutdown = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            shutdown = true;
            return List.of();
        }

        @Override
        public boolean isShutdown() {
            return shutdown;
        }

        @Override
        public boolean isTerminated() {
            return shutdown;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return true;
        }

        @Override
        public void execute(Runnable command) {
            command.run();
        }
    }
}
