package com.whispernetwork.api.interfaces.http.controller;

import com.whispernetwork.api.application.dto.SimulationRunSnapshot;
import com.whispernetwork.api.application.dto.SimulationTimelineEventSnapshot;
import com.whispernetwork.api.application.ports.out.SimulationEventStreamPort;
import com.whispernetwork.api.application.security.ActorContext;
import com.whispernetwork.api.application.services.simulation.SimulationCommandApplicationService;
import com.whispernetwork.api.application.services.simulation.SimulationQueryApplicationService;
import com.whispernetwork.api.interfaces.http.dto.CancelSimulationRequest;
import com.whispernetwork.api.interfaces.http.dto.SimulationRunStatusResponse;
import com.whispernetwork.api.interfaces.http.dto.StartSimulationRequest;
import com.whispernetwork.api.interfaces.http.dto.StartSimulationResponse;
import com.whispernetwork.api.interfaces.http.mapper.SimulationHttpMapper;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * HTTP controller for simulation APIs.
 *
 * Responsibility is intentionally limited to validation, mapping and response shaping.
 */
@RestController
@RequestMapping("/api/simulations")
public class SimulationController {
    private final SimulationCommandApplicationService commandService;
    private final SimulationQueryApplicationService queryService;
    private final SimulationEventStreamPort eventStream;
    private final ActorContext actorContext;
    private final SimulationHttpMapper mapper;

    /**
     * Creates simulation controller.
     */
    public SimulationController(
            SimulationCommandApplicationService commandService,
            SimulationQueryApplicationService queryService,
            SimulationEventStreamPort eventStream,
            ActorContext actorContext,
            SimulationHttpMapper mapper) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.eventStream = eventStream;
        this.actorContext = actorContext;
        this.mapper = mapper;
    }

    /**
     * Requests simulation execution.
     */
    @PostMapping
    public ResponseEntity<StartSimulationResponse> startSimulation(@Valid @RequestBody StartSimulationRequest request) {
        String runId = commandService.startSimulation(mapper.toStartCommand(request));
        return ResponseEntity.accepted().body(mapper.toStartResponse(runId));
    }

    /**
     * Requests cancellation of an existing run.
     */
    @PostMapping("/{runId}/cancel")
    public ResponseEntity<Void> cancelSimulation(
            @PathVariable String runId, @Valid @RequestBody CancelSimulationRequest request) {
        boolean found = commandService.cancelSimulation(runId, mapper.toCancelCommand(request));
        if (!found) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Simulation run not found");
        }
        return ResponseEntity.accepted().build();
    }

    /**
     * Returns current run status.
     */
    @GetMapping("/{runId}")
    public SimulationRunStatusResponse getSimulationStatus(@PathVariable String runId) {
        Optional<SimulationRunSnapshot> run = queryService.findRun(runId);
        if (run.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Simulation run not found");
        }

        return mapper.toStatusResponse(run.get());
    }

    /**
     * Returns timeline events for monitoring.
     */
    @GetMapping("/{runId}/events")
    public List<SimulationTimelineEventSnapshot> getSimulationEvents(@PathVariable String runId) {
        return queryService.listTimeline(runId);
    }

    /**
     * Streams live timeline events for monitoring.
     */
    @GetMapping(value = "/{runId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamSimulationEvents(@PathVariable String runId) {
        String ownerId = actorContext.currentActorId();
        List<SimulationTimelineEventSnapshot> replay = queryService.listTimeline(runId);
        return eventStream.open(ownerId, runId, replay);
    }
}
