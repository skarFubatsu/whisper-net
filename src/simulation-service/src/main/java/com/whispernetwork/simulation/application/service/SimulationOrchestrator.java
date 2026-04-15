package com.whispernetwork.simulation.application.service;

import com.whispernetwork.shared.dto.RunStatus;
import com.whispernetwork.simulation.application.model.IdempotencyKey;
import com.whispernetwork.simulation.application.model.SimulationCancelCommand;
import com.whispernetwork.simulation.application.model.SimulationEvent;
import com.whispernetwork.simulation.application.model.SimulationRun;
import com.whispernetwork.simulation.application.model.SimulationStartCommand;
import com.whispernetwork.simulation.application.port.in.SimulationCommandPort;
import com.whispernetwork.simulation.application.port.out.InfluenceNetworkProvider;
import com.whispernetwork.simulation.application.port.out.SimulationEventPort;
import com.whispernetwork.simulation.application.port.out.SimulationRunRepository;
import com.whispernetwork.simulation.core.engine.SimulationTickEngine;
import com.whispernetwork.simulation.core.engine.SimulationTickResult;
import com.whispernetwork.simulation.core.model.InfluenceNetwork;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Application service coordinating simulation run lifecycle and background execution.
 */
public final class SimulationOrchestrator implements SimulationCommandPort, AutoCloseable {
    private final SimulationTickEngine tickEngine;
    private final SimulationRunRepository runRepository;
    private final InfluenceNetworkProvider networkProvider;
    private final SimulationEventPort eventPort;
    private final ExecutorService executorService;
    private final Map<String, Future<?>> runningFutures;

    /**
     * Creates orchestrator with default thread pool sizing.
     */
    public SimulationOrchestrator(
            SimulationTickEngine tickEngine,
            SimulationRunRepository runRepository,
            InfluenceNetworkProvider networkProvider,
            SimulationEventPort eventPort) {
        this(tickEngine, runRepository, networkProvider, eventPort, Executors.newCachedThreadPool());
    }

    /**
     * Creates orchestrator with custom executor.
     */
    public SimulationOrchestrator(
            SimulationTickEngine tickEngine,
            SimulationRunRepository runRepository,
            InfluenceNetworkProvider networkProvider,
            SimulationEventPort eventPort,
            ExecutorService executorService) {
        this.tickEngine = tickEngine;
        this.runRepository = runRepository;
        this.networkProvider = networkProvider;
        this.eventPort = eventPort;
        this.executorService = executorService;
        this.runningFutures = new ConcurrentHashMap<>();
    }

    @Override
    public String requestSimulation(SimulationStartCommand command) {
        IdempotencyKey key = IdempotencyKey.forStart(command);
        Optional<String> existing = runRepository.findByIdempotency(key);
        if (existing.isPresent()) {
            return existing.get();
        }

        runRepository.findActiveByNetwork(command.networkId()).ifPresent(activeRun -> {
            throw new IllegalStateException("Active run already exists for network: " + activeRun.getId());
        });

        String runId = UUID.randomUUID().toString();
        SimulationRun run = new SimulationRun(
                runId,
                command.networkId(),
                command.networkVersionNumber(),
                command.requestedByActorId(),
                command.clientRequestId(),
                command.requestedTicks());

        runRepository.save(run);
        runRepository.putIdempotency(key, runId);

        Future<?> future = executorService.submit(() -> executeRun(runId));
        runningFutures.put(runId, future);
        return runId;
    }

    @Override
    public boolean requestCancellation(SimulationCancelCommand command) {
        IdempotencyKey key = IdempotencyKey.forCancel(command);
        if (runRepository.findByIdempotency(key).isPresent()) {
            return true;
        }

        Optional<SimulationRun> runOptional = runRepository.findById(command.simulationRunId());
        if (runOptional.isEmpty()) {
            return false;
        }

        SimulationRun run = runOptional.get();
        if (!run.getNetworkId().equals(command.networkId())) {
            return false;
        }

        runRepository.putIdempotency(key, run.getId());
        run.markCancelling(command.requestedByActorId(), command.clientRequestId());
        runRepository.save(run);
        return true;
    }

    @Override
    public Optional<SimulationRun> findRun(String runId) {
        return runRepository.findById(runId);
    }

    private void executeRun(String runId) {
        SimulationRun run = runRepository
                .findById(runId)
                .orElseThrow(() -> new IllegalStateException("Run not found for id=" + runId));

        try {
            if (run.getStatus() == RunStatus.CANCELLING) {
                run.markCancelled();
                runRepository.save(run);
                eventPort.publish(new SimulationEvent.SimulationCancelled(
                        run.getId(),
                        run.getNetworkId(),
                        cancellationActorIdFor(run),
                        cancellationClientRequestIdFor(run),
                        run.getRequestedByActorId(),
                        run.getClientRequestId(),
                        Instant.now()));
                return;
            }

            run.markRunning();
            runRepository.save(run);
            eventPort.publish(new SimulationEvent.SimulationStarted(
                    run.getId(),
                    run.getNetworkId(),
                    run.getNetworkVersionNumber(),
                    run.getRequestedByActorId(),
                    run.getClientRequestId(),
                    run.getRequestedByActorId(),
                    run.getClientRequestId(),
                    Instant.now()));

            InfluenceNetwork network =
                    networkProvider.loadNetworkSnapshot(run.getNetworkId(), run.getNetworkVersionNumber());

            for (int tick = 1; tick <= run.getRequestedTicks(); tick++) {
                if (run.getStatus() == RunStatus.CANCELLING) {
                    run.markCancelled();
                    runRepository.save(run);
                    eventPort.publish(new SimulationEvent.SimulationCancelled(
                            run.getId(),
                            run.getNetworkId(),
                            cancellationActorIdFor(run),
                            cancellationClientRequestIdFor(run),
                            run.getRequestedByActorId(),
                            run.getClientRequestId(),
                            Instant.now()));
                    return;
                }

                SimulationTickResult tickResult = tickEngine.executeTick(network, tick);
                run.incrementCompletedTicks();
                runRepository.save(run);
                eventPort.publish(new SimulationEvent.SimulationTickCompleted(
                        run.getId(),
                        run.getNetworkId(),
                        tick,
                        tickResult.updates().size(),
                        tickResult.updates(),
                        run.getRequestedByActorId(),
                        run.getClientRequestId(),
                        Instant.now()));
            }

            if (run.getStatus() == RunStatus.CANCELLING) {
                run.markCancelled();
                runRepository.save(run);
                eventPort.publish(new SimulationEvent.SimulationCancelled(
                        run.getId(),
                        run.getNetworkId(),
                        cancellationActorIdFor(run),
                        cancellationClientRequestIdFor(run),
                        run.getRequestedByActorId(),
                        run.getClientRequestId(),
                        Instant.now()));
            } else {
                run.markCompleted();
                runRepository.save(run);
                eventPort.publish(new SimulationEvent.SimulationCompleted(
                        run.getId(),
                        run.getNetworkId(),
                        run.getCompletedTicks(),
                        run.getRequestedByActorId(),
                        run.getClientRequestId(),
                        Instant.now()));
            }
        } catch (RuntimeException ex) {
            run.markFailed(ex.getMessage());
            runRepository.save(run);
            eventPort.publish(new SimulationEvent.SimulationFailed(
                    run.getId(),
                    run.getNetworkId(),
                    ex.getMessage(),
                    run.getRequestedByActorId(),
                    run.getClientRequestId(),
                    Instant.now()));
        } finally {
            runningFutures.remove(runId);
        }
    }

    @Override
    public void close() {
        executorService.shutdownNow();
    }

    private static String cancellationActorIdFor(SimulationRun run) {
        String actorId = run.getCancellationRequestedByActorId();
        return actorId != null && !actorId.isBlank() ? actorId : run.getRequestedByActorId();
    }

    private static String cancellationClientRequestIdFor(SimulationRun run) {
        String clientRequestId = run.getCancellationClientRequestId();
        return clientRequestId != null && !clientRequestId.isBlank() ? clientRequestId : run.getClientRequestId();
    }
}
