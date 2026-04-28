package com.whispernetwork.api.interfaces.http.controller;

import com.whispernetwork.api.application.security.ActorContext;
import com.whispernetwork.api.application.services.catalog.NetworkCatalogApplicationService;
import com.whispernetwork.api.infrastructure.stream.NetworkSseBroadcaster;
import com.whispernetwork.api.interfaces.http.dto.AgentPageResponse;
import com.whispernetwork.api.interfaces.http.dto.AgentResponse;
import com.whispernetwork.api.interfaces.http.dto.NetworkAgentRequest;
import com.whispernetwork.api.interfaces.http.dto.NetworkRequest;
import com.whispernetwork.api.interfaces.http.dto.NetworkResponse;
import com.whispernetwork.api.interfaces.http.dto.NetworkVersionRequest;
import com.whispernetwork.api.interfaces.http.dto.NetworkVersionResponse;
import com.whispernetwork.api.interfaces.http.dto.RelationshipPageResponse;
import com.whispernetwork.api.interfaces.http.dto.RelationshipResponse;
import com.whispernetwork.api.interfaces.http.dto.RelationshipSnapshotRequest;
import com.whispernetwork.api.interfaces.http.mapper.AgentHttpMapper;
import com.whispernetwork.api.interfaces.http.mapper.NetworkHttpMapper;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/networks")
public class NetworkController {
    private final NetworkCatalogApplicationService service;
    private final NetworkSseBroadcaster broadcaster;
    private final ActorContext actorContext;
    private final NetworkHttpMapper networkMapper;
    private final AgentHttpMapper agentMapper;

    public NetworkController(
            NetworkCatalogApplicationService service,
            NetworkSseBroadcaster broadcaster,
            ActorContext actorContext,
            NetworkHttpMapper networkMapper,
            AgentHttpMapper agentMapper) {
        this.service = service;
        this.broadcaster = broadcaster;
        this.actorContext = actorContext;
        this.networkMapper = networkMapper;
        this.agentMapper = agentMapper;
    }

    @PostMapping
    public ResponseEntity<NetworkResponse> createNetwork(@Valid @RequestBody NetworkRequest request) {
        NetworkResponse created = networkMapper.toResponse(service.createNetwork(networkMapper.toCommand(request)));
        URI loc = URI.create(String.format("/api/networks/%s", created.networkId()));
        return ResponseEntity.created(Objects.requireNonNull(loc)).body(created);
    }

    @GetMapping
    public List<NetworkResponse> listNetworks() {
        return networkMapper.toResponses(service.listNetworks());
    }

    @GetMapping("/{networkId}/agents")
    public AgentPageResponse listNetworkAgents(
            @PathVariable String networkId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return agentMapper.toPageResponse(service.listNetworkAgents(networkId, page, size));
    }

    @PostMapping("/{networkId}/agents")
    public ResponseEntity<AgentResponse> deployNetworkAgent(
            @PathVariable String networkId, @Valid @RequestBody NetworkAgentRequest request) {
        AgentResponse deployed =
                agentMapper.toResponse(service.deployAgent(networkId, networkMapper.toCommand(request)));
        URI loc = URI.create(String.format("/api/networks/%s/agents/%s", networkId, deployed.agentId()));
        return ResponseEntity.created(Objects.requireNonNull(loc)).body(deployed);
    }

    @DeleteMapping("/{networkId}/agents/{agentId}")
    public ResponseEntity<Void> undeployNetworkAgent(@PathVariable String networkId, @PathVariable String agentId) {
        service.undeployAgent(networkId, agentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{networkId}/relationships")
    public RelationshipPageResponse listNetworkRelationships(
            @PathVariable String networkId,
            @RequestParam(required = false) String sourceAgentId,
            @RequestParam(required = false) String targetAgentId,
            @RequestParam(required = false) Boolean mirror,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return networkMapper.toPageResponse(
                service.listNetworkRelationships(networkId, page, size, sourceAgentId, targetAgentId, mirror));
    }

    @PostMapping("/{networkId}/relationships")
    public ResponseEntity<RelationshipResponse> createNetworkRelationship(
            @PathVariable String networkId,
            @Valid @RequestBody RelationshipSnapshotRequest request,
            @RequestParam(defaultValue = "true") boolean mirror) {
        RelationshipResponse created = networkMapper.toResponse(
                service.createRelationship(networkId, networkMapper.toCommand(request), mirror));
        URI loc = URI.create(String.format("/api/networks/%s/relationships/%s", networkId, created.relationshipId()));
        return ResponseEntity.created(Objects.requireNonNull(loc)).body(created);
    }

    @DeleteMapping("/{networkId}/relationships/{relationshipId}")
    public ResponseEntity<Void> deleteNetworkRelationship(
            @PathVariable String networkId, @PathVariable String relationshipId) {
        service.deleteRelationship(networkId, relationshipId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{networkId}/versions")
    public ResponseEntity<NetworkVersionResponse> createVersion(
            @PathVariable String networkId, @Valid @RequestBody NetworkVersionRequest request) {
        NetworkVersionResponse created =
                networkMapper.toResponse(service.createVersion(networkId, networkMapper.toCommand(request)));
        URI loc = URI.create(String.format("/api/networks/%s/versions/%d", networkId, created.versionNumber()));
        return ResponseEntity.created(Objects.requireNonNull(loc)).body(created);
    }

    @GetMapping("/{networkId}/versions")
    public List<NetworkVersionResponse> listVersions(@PathVariable String networkId) {
        return networkMapper.toVersionResponses(service.listVersions(networkId));
    }

    @GetMapping("/{networkId}/versions/{versionNumber}")
    public NetworkVersionResponse getVersion(@PathVariable String networkId, @PathVariable int versionNumber) {
        return service.getVersion(networkId, versionNumber)
                .map(networkMapper::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Version not found"));
    }

    @GetMapping(value = "/{networkId}/stream", produces = "text/event-stream")
    public SseEmitter streamNetwork(
            @PathVariable String networkId,
            @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId) {
        String snapshotJson = Objects.requireNonNull(service.getDraftSnapshotJson(networkId));
        String owner = actorContext.currentActorId();
        SseEmitter emitter = broadcaster.subscribe(owner, networkId, lastEventId);
        try {
            if (!broadcaster.isValidLastEventId(lastEventId)) {
                SseEmitter.SseEventBuilder warningEvent = SseEmitter.event()
                        .name("stream-warning")
                        .data(
                                "{\"code\":\"invalid-last-event-id\",\"message\":\"Last-Event-ID is invalid; resuming from current stream\"}");
                emitter.send(warningEvent);
            }
            SseEmitter.SseEventBuilder event =
                    SseEmitter.event().name("snapshot").data(Objects.requireNonNull(snapshotJson));
            emitter.send(event);
        } catch (Exception ex) {
            emitter.completeWithError(ex);
        }
        return emitter;
    }
}
