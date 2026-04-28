package com.whispernetwork.api.interfaces.http.mapper;

import com.whispernetwork.api.application.dto.catalog.AgentSnapshot;
import com.whispernetwork.api.application.dto.catalog.NetworkAgentCommand;
import com.whispernetwork.api.application.dto.catalog.NetworkCommand;
import com.whispernetwork.api.application.dto.catalog.NetworkVersionCommand;
import com.whispernetwork.api.application.dto.catalog.NetworkVersionView;
import com.whispernetwork.api.application.dto.catalog.NetworkView;
import com.whispernetwork.api.application.dto.catalog.PageResult;
import com.whispernetwork.api.application.dto.catalog.RelationshipCommand;
import com.whispernetwork.api.application.dto.catalog.RelationshipSnapshot;
import com.whispernetwork.api.application.dto.catalog.RelationshipView;
import com.whispernetwork.api.interfaces.http.dto.AgentSnapshotRequest;
import com.whispernetwork.api.interfaces.http.dto.NetworkAgentRequest;
import com.whispernetwork.api.interfaces.http.dto.NetworkRequest;
import com.whispernetwork.api.interfaces.http.dto.NetworkResponse;
import com.whispernetwork.api.interfaces.http.dto.NetworkVersionRequest;
import com.whispernetwork.api.interfaces.http.dto.NetworkVersionResponse;
import com.whispernetwork.api.interfaces.http.dto.RelationshipPageResponse;
import com.whispernetwork.api.interfaces.http.dto.RelationshipResponse;
import com.whispernetwork.api.interfaces.http.dto.RelationshipSnapshotRequest;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class NetworkHttpMapper {
    public NetworkCommand toCommand(NetworkRequest request) {
        return new NetworkCommand(request.name(), request.networkId());
    }

    public NetworkResponse toResponse(NetworkView view) {
        return new NetworkResponse(view.networkId(), view.name(), view.createdAt(), view.updatedAt());
    }

    public List<NetworkResponse> toResponses(List<NetworkView> views) {
        return views.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public NetworkAgentCommand toCommand(NetworkAgentRequest request) {
        return new NetworkAgentCommand(request.agentId());
    }

    public NetworkVersionCommand toCommand(NetworkVersionRequest request) {
        List<AgentSnapshot> agents =
                request.agents().stream().map(this::toAgentSnapshot).collect(Collectors.toList());
        List<RelationshipSnapshot> relationships = request.relationships().stream()
                .map(this::toRelationshipSnapshot)
                .collect(Collectors.toList());
        return new NetworkVersionCommand(request.description(), request.baseVersionNumber(), agents, relationships);
    }

    public NetworkVersionResponse toResponse(NetworkVersionView view) {
        return new NetworkVersionResponse(
                view.networkId(), view.versionNumber(), view.description(), view.createdBy(), view.createdAt());
    }

    public List<NetworkVersionResponse> toVersionResponses(List<NetworkVersionView> views) {
        return views.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public RelationshipCommand toCommand(RelationshipSnapshotRequest request) {
        return new RelationshipCommand(
                request.relationshipId(),
                request.mirrorGroupId(),
                request.reverseRelationshipId(),
                request.sourceAgentId(),
                request.targetAgentId(),
                request.weight(),
                request.trustValue(),
                request.relationshipType(),
                request.transmissionMode());
    }

    public RelationshipResponse toResponse(RelationshipView view) {
        return new RelationshipResponse(
                view.relationshipId(),
                view.mirrorGroupId(),
                view.reverseRelationshipId(),
                view.sourceAgentId(),
                view.targetAgentId(),
                view.weight(),
                view.trustValue(),
                view.relationshipType(),
                view.transmissionMode());
    }

    public RelationshipPageResponse toPageResponse(PageResult<RelationshipView> page) {
        List<RelationshipResponse> items =
                page.items().stream().map(this::toResponse).collect(Collectors.toList());
        return new RelationshipPageResponse(items, page.page(), page.size(), page.total());
    }

    private AgentSnapshot toAgentSnapshot(AgentSnapshotRequest request) {
        return new AgentSnapshot(
                request.agentId(),
                request.nickname(),
                request.role(),
                request.bias(),
                request.stubbornness(),
                request.susceptibility(),
                request.suspiciousness());
    }

    private RelationshipSnapshot toRelationshipSnapshot(RelationshipSnapshotRequest request) {
        return new RelationshipSnapshot(
                request.relationshipId(),
                request.mirrorGroupId(),
                request.reverseRelationshipId(),
                request.sourceAgentId(),
                request.targetAgentId(),
                request.weight(),
                request.trustValue(),
                request.relationshipType(),
                request.transmissionMode());
    }
}
