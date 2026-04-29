package com.whispernetwork.api.interfaces.http.mapper;

import com.whispernetwork.api.application.dto.CancelSimulationCommand;
import com.whispernetwork.api.application.dto.SimulationRunSnapshot;
import com.whispernetwork.api.application.dto.StartSimulationCommand;
import com.whispernetwork.api.interfaces.http.dto.CancelSimulationRequest;
import com.whispernetwork.api.interfaces.http.dto.SimulationRunStatusResponse;
import com.whispernetwork.api.interfaces.http.dto.StartSimulationRequest;
import com.whispernetwork.api.interfaces.http.dto.StartSimulationResponse;
import org.springframework.stereotype.Component;

@Component
public class SimulationHttpMapper {

    public StartSimulationCommand toStartCommand(StartSimulationRequest request) {
        return new StartSimulationCommand(
                request.networkId(),
                request.networkVersionNumber(),
                request.ownerId(),
                request.actorId(),
                request.clientRequestId(),
                request.requestedTicks());
    }

    public CancelSimulationCommand toCancelCommand(CancelSimulationRequest request) {
        return new CancelSimulationCommand(request.ownerId(), request.actorId(), request.clientRequestId());
    }

    public StartSimulationResponse toStartResponse(String runId) {
        return new StartSimulationResponse(runId);
    }

    public SimulationRunStatusResponse toStatusResponse(SimulationRunSnapshot run) {
        return new SimulationRunStatusResponse(
                run.runId(),
                run.networkId(),
                run.networkVersionNumber(),
                run.status(),
                run.completedTicks(),
                run.requestedTicks(),
                run.failureMessage(),
                run.createdAt(),
                run.updatedAt());
    }
}
