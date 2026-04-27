package com.whispernetwork.api.interfaces.http.controller;

import com.whispernetwork.api.application.services.catalog.AgentCatalogApplicationService;
import com.whispernetwork.api.interfaces.http.dto.AgentPageResponse;
import com.whispernetwork.api.interfaces.http.dto.AgentRequest;
import com.whispernetwork.api.interfaces.http.dto.AgentResponse;
import com.whispernetwork.api.interfaces.http.mapper.AgentHttpMapper;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/agents")
public class AgentController {
    private final AgentCatalogApplicationService service;
    private final AgentHttpMapper mapper;

    public AgentController(AgentCatalogApplicationService service, AgentHttpMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public AgentPageResponse listAgents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) Boolean deployed,
            @RequestParam(required = false) String networkId) {
        return mapper.toPageResponse(service.listAgents(page, size, deployed, networkId));
    }

    @PostMapping
    public ResponseEntity<AgentResponse> createAgent(@Valid @RequestBody AgentRequest request) {
        AgentResponse created = mapper.toResponse(service.createAgent(mapper.toCommand(request)));
        URI location = URI.create(String.format("/api/agents/%s", created.agentId()));
        return ResponseEntity.created(Objects.requireNonNull(location)).body(created);
    }

    @GetMapping("/{agentId}")
    public AgentResponse getAgent(@PathVariable String agentId) {
        return service.findAgent(agentId)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent not found"));
    }

    @PutMapping("/{agentId}")
    public AgentResponse updateAgent(@PathVariable String agentId, @Valid @RequestBody AgentRequest request) {
        return service.updateAgent(agentId, mapper.toCommand(request))
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent not found"));
    }
}
