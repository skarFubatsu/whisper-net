package com.whispernetwork.api.application.ports.out;

import com.whispernetwork.api.application.dto.CancelSimulationCommand;
import com.whispernetwork.api.application.dto.StartSimulationCommand;

/**
 * Publishes simulation commands to the external command bus.
 */
public interface SimulationCommandPublisherPort {

    void publishStart(StartSimulationCommand command);

    void publishCancel(String runId, String networkId, CancelSimulationCommand command);
}
