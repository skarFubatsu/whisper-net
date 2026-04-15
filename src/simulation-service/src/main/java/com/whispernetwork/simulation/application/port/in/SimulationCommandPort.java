package com.whispernetwork.simulation.application.port.in;

import com.whispernetwork.simulation.application.model.SimulationCancelCommand;
import com.whispernetwork.simulation.application.model.SimulationRun;
import com.whispernetwork.simulation.application.model.SimulationStartCommand;
import java.util.Optional;

/**
 * Command entry point for simulation lifecycle operations.
 */
public interface SimulationCommandPort {

    /**
     * Requests simulation execution.
     *
     * @param command start command
     * @return run id
     */
    String requestSimulation(SimulationStartCommand command);

    /**
     * Requests cancellation of an active simulation.
     *
     * @param command cancel command
     * @return true when a run was found, false otherwise
     */
    boolean requestCancellation(SimulationCancelCommand command);

    /**
     * Returns run by id.
     *
     * @param runId run id
     * @return optional run
     */
    Optional<SimulationRun> findRun(String runId);
}
