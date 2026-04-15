package com.whispernetwork.simulation.infrastructure.sqlite;

import com.whispernetwork.simulation.application.model.SimulationEvent;
import com.whispernetwork.simulation.application.port.out.SimulationEventPort;
import com.whispernetwork.simulation.application.port.out.SimulationHistoryRepository;

/**
 * Event publisher decorator that persists history before publishing to RabbitMQ.
 */
public final class PersistingSimulationEventPublisher implements SimulationEventPort {
    private final SimulationHistoryRepository historyRepository;
    private final SimulationEventPort delegate;

    /**
     * Creates decorator instance.
     */
    public PersistingSimulationEventPublisher(
            SimulationHistoryRepository historyRepository, SimulationEventPort delegate) {
        this.historyRepository = historyRepository;
        this.delegate = delegate;
    }

    @Override
    public void publish(SimulationEvent event) {
        historyRepository.record(event);
        delegate.publish(event);
    }
}
