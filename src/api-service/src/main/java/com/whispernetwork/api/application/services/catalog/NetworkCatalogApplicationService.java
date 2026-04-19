package com.whispernetwork.api.application.services.catalog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whispernetwork.api.application.dto.catalog.AgentSnapshot;
import com.whispernetwork.api.application.dto.catalog.AgentView;
import com.whispernetwork.api.application.dto.catalog.NetworkAgentCommand;
import com.whispernetwork.api.application.dto.catalog.NetworkCommand;
import com.whispernetwork.api.application.dto.catalog.NetworkSnapshotRecord;
import com.whispernetwork.api.application.dto.catalog.NetworkVersionCommand;
import com.whispernetwork.api.application.dto.catalog.NetworkVersionView;
import com.whispernetwork.api.application.dto.catalog.NetworkView;
import com.whispernetwork.api.application.dto.catalog.PageResult;
import com.whispernetwork.api.application.dto.catalog.RelationshipCommand;
import com.whispernetwork.api.application.dto.catalog.RelationshipSnapshot;
import com.whispernetwork.api.application.dto.catalog.RelationshipView;
import com.whispernetwork.api.application.error.BadRequestException;
import com.whispernetwork.api.application.error.ConflictException;
import com.whispernetwork.api.application.error.NotFoundException;
import com.whispernetwork.api.application.ports.out.AgentCatalogPort;
import com.whispernetwork.api.application.ports.out.NetworkCatalogPort;
import com.whispernetwork.api.application.ports.out.NetworkSnapshotPort;
import com.whispernetwork.api.application.ports.out.NetworkSsePublisherPort;
import com.whispernetwork.api.application.security.ActorContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NetworkCatalogApplicationService {
    private static final int DRAFT_VERSION_NUMBER = 0;

    private final NetworkCatalogPort networkStore;
    private final NetworkSnapshotPort snapshotStore;
    private final AgentCatalogPort agentStore;
    private final ActorContext actorContext;
    private final ObjectMapper objectMapper;
    private final NetworkSsePublisherPort ssePublisher;

    public NetworkCatalogApplicationService(
            NetworkCatalogPort networkStore,
            NetworkSnapshotPort snapshotStore,
            AgentCatalogPort agentStore,
            ActorContext actorContext,
            ObjectMapper objectMapper,
            NetworkSsePublisherPort ssePublisher) {
        this.networkStore = networkStore;
        this.snapshotStore = snapshotStore;
        this.agentStore = agentStore;
        this.actorContext = actorContext;
        this.objectMapper = objectMapper;
        this.ssePublisher = ssePublisher;
    }

    @Transactional
    public NetworkView createNetwork(NetworkCommand command) {
        String owner = actorContext.currentActorId();
        String networkId = command.networkId() == null || command.networkId().isBlank()
                ? UUID.randomUUID().toString()
                : command.networkId();

        Instant now = Instant.now();
        NetworkView toCreate = new NetworkView(networkId, command.name(), now, now);
        NetworkView created = networkStore.create(owner, toCreate);
        saveDraftSnapshot(owner, networkId, emptySnapshotDocument());
        return created;
    }

    @Transactional(readOnly = true)
    public List<NetworkView> listNetworks() {
        String owner = actorContext.currentActorId();
        return networkStore.findByOwnerId(owner);
    }

    @Transactional(readOnly = true)
    public PageResult<AgentView> listNetworkAgents(String networkId, int page, int size) {
        PaginationValidator.validate(page, size);

        String owner = actorContext.currentActorId();
        requireNetwork(owner, networkId);

        NetworkSnapshotDocument draft = loadDraftSnapshot(owner, networkId);
        List<AgentSnapshot> agents = draft.agents() == null ? List.of() : draft.agents();
        int total = agents.size();
        int fromIndex = Math.min(page * size, total);
        int toIndex = Math.min(fromIndex + size, total);
        List<AgentView> items = agents.subList(fromIndex, toIndex).stream()
                .map(snapshot -> agentStore
                        .findByOwnerIdAndAgentId(owner, snapshot.agentId())
                        .orElseThrow(
                                () -> new ConflictException("Draft references missing agent: " + snapshot.agentId())))
                .collect(Collectors.toList());

        return new PageResult<>(items, page, size, total);
    }

    @Transactional
    public AgentView deployAgent(String networkId, NetworkAgentCommand command) {
        String owner = actorContext.currentActorId();
        requireNetwork(owner, networkId);

        AgentView agent = agentStore
                .findByOwnerIdAndAgentId(owner, command.agentId())
                .orElseThrow(() -> new NotFoundException("Agent not found"));

        NetworkSnapshotDocument draft = loadDraftSnapshot(owner, networkId);
        List<AgentSnapshot> agents = new ArrayList<>(draft.agents() == null ? List.of() : draft.agents());
        boolean alreadyDeployed = agents.stream().anyMatch(a -> a.agentId().equals(agent.agentId()));
        if (alreadyDeployed) {
            throw new ConflictException("Agent already deployed in draft");
        }

        agents.add(toSnapshot(agent));
        saveDraftSnapshot(
                owner, networkId, new NetworkSnapshotDocument(agents, normalizeRelationships(draft.relationships())));
        try {
            ssePublisher.publishEvent(owner, networkId, "agent.deployed", agent);
        } catch (Exception ignored) {
        }
        return agent;
    }

    @Transactional
    public void undeployAgent(String networkId, String agentId) {
        String owner = actorContext.currentActorId();
        requireNetwork(owner, networkId);

        NetworkSnapshotDocument draft = loadDraftSnapshot(owner, networkId);
        List<AgentSnapshot> agents = new ArrayList<>(draft.agents() == null ? List.of() : draft.agents());
        boolean removed = agents.removeIf(a -> a.agentId().equals(agentId));
        if (!removed) {
            throw new NotFoundException("Agent not deployed in draft");
        }

        saveDraftSnapshot(
                owner, networkId, new NetworkSnapshotDocument(agents, normalizeRelationships(draft.relationships())));
        try {
            ssePublisher.publishEvent(owner, networkId, "agent.undeployed", java.util.Map.of("agentId", agentId));
        } catch (Exception ignored) {
        }
    }

    @Transactional(readOnly = true)
    public PageResult<RelationshipView> listNetworkRelationships(
            String networkId, int page, int size, String sourceAgentId, String targetAgentId, Boolean mirror) {
        PaginationValidator.validate(page, size);

        String owner = actorContext.currentActorId();
        requireNetwork(owner, networkId);

        NetworkSnapshotDocument draft = loadDraftSnapshot(owner, networkId);
        List<RelationshipSnapshot> relationships = draft.relationships() == null ? List.of() : draft.relationships();
        List<RelationshipSnapshot> filtered = relationships.stream()
                .filter(relationship -> sourceAgentId == null
                        || sourceAgentId.isBlank()
                        || sourceAgentId.equals(relationship.sourceAgentId()))
                .filter(relationship -> targetAgentId == null
                        || targetAgentId.isBlank()
                        || targetAgentId.equals(relationship.targetAgentId()))
                .filter(relationship -> mirror == null || mirror.booleanValue() == isMirrored(relationship))
                .toList();

        int total = filtered.size();
        int fromIndex = Math.min(page * size, total);
        int toIndex = Math.min(fromIndex + size, total);
        List<RelationshipView> items =
                filtered.subList(fromIndex, toIndex).stream().map(this::toView).collect(Collectors.toList());
        return new PageResult<>(items, page, size, total);
    }

    @Transactional
    public RelationshipView createRelationship(String networkId, RelationshipCommand request, boolean mirror) {
        String owner = actorContext.currentActorId();
        requireNetwork(owner, networkId);

        String sourceAgentId = requireText(request.sourceAgentId(), "sourceAgentId");
        String targetAgentId = requireText(request.targetAgentId(), "targetAgentId");
        if (sourceAgentId.equals(targetAgentId)) {
            throw new BadRequestException("Self-relationships are not allowed");
        }

        NetworkSnapshotDocument draft = loadDraftSnapshot(owner, networkId);
        ensureAgentsAreDeployed(owner, draft, sourceAgentId, targetAgentId);

        List<RelationshipSnapshot> relationships =
                new ArrayList<>(draft.relationships() == null ? List.of() : draft.relationships());
        String unorderedPairKey = pairKey(sourceAgentId, targetAgentId);
        if (relationships.stream()
                .anyMatch(existing -> pairKey(existing.sourceAgentId(), existing.targetAgentId())
                                .equals(unorderedPairKey)
                        || sameRelationshipId(existing, request.relationshipId()))) {
            throw new ConflictException("Relationship already exists in draft");
        }

        String relationshipId = hasText(request.relationshipId())
                ? request.relationshipId()
                : UUID.randomUUID().toString();
        String mirrorGroupId =
                mirror ? firstText(request.mirrorGroupId(), UUID.randomUUID().toString()) : null;

        RelationshipSnapshot forward = new RelationshipSnapshot(
                relationshipId,
                mirrorGroupId,
                mirror ? UUID.randomUUID().toString() : null,
                sourceAgentId,
                targetAgentId,
                request.weight(),
                request.trustValue(),
                request.relationshipType(),
                request.transmissionMode());
        relationships.add(forward);

        if (mirror) {
            String reverseRelationshipId = forward.reverseRelationshipId();
            RelationshipSnapshot reverse = new RelationshipSnapshot(
                    reverseRelationshipId,
                    mirrorGroupId,
                    forward.relationshipId(),
                    targetAgentId,
                    sourceAgentId,
                    request.weight(),
                    request.trustValue(),
                    request.relationshipType(),
                    request.transmissionMode());
            relationships.add(reverse);
            saveDraftSnapshot(owner, networkId, new NetworkSnapshotDocument(draft.agents(), relationships));
        } else {
            saveDraftSnapshot(owner, networkId, new NetworkSnapshotDocument(draft.agents(), relationships));
        }

        try {
            ssePublisher.publishEvent(owner, networkId, "relationship.created", toView(forward));
        } catch (Exception ignored) {
        }

        return toView(forward);
    }

    @Transactional
    public void deleteRelationship(String networkId, String relationshipId) {
        String owner = actorContext.currentActorId();
        requireNetwork(owner, networkId);

        NetworkSnapshotDocument draft = loadDraftSnapshot(owner, networkId);
        List<RelationshipSnapshot> relationships =
                new ArrayList<>(draft.relationships() == null ? List.of() : draft.relationships());
        RelationshipSnapshot target = relationships.stream()
                .filter(relationship -> relationship.relationshipId() != null
                        && relationship.relationshipId().equals(relationshipId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Relationship not found"));

        String mirrorGroupId = target.mirrorGroupId();
        String reverseRelationshipId = target.reverseRelationshipId();
        relationships.removeIf(relationship -> relationship.relationshipId() != null
                && (relationship.relationshipId().equals(relationshipId)
                        || (reverseRelationshipId != null
                                && !reverseRelationshipId.isBlank()
                                && relationship.relationshipId().equals(reverseRelationshipId))
                        || (mirrorGroupId != null
                                && !mirrorGroupId.isBlank()
                                && mirrorGroupId.equals(relationship.mirrorGroupId()))));

        saveDraftSnapshot(owner, networkId, new NetworkSnapshotDocument(draft.agents(), relationships));
        try {
            ssePublisher.publishEvent(
                    owner, networkId, "relationship.deleted", java.util.Map.of("relationshipId", relationshipId));
        } catch (Exception ignored) {
        }
    }

    @Transactional
    public NetworkVersionView createVersion(String networkId, NetworkVersionCommand command) {
        String owner = actorContext.currentActorId();
        requireNetwork(owner, networkId);

        int latestVersion = networkStore.countVersions(owner, networkId);
        if (command.baseVersionNumber() != null && command.baseVersionNumber() != latestVersion) {
            throw new ConflictException("Base version mismatch");
        }

        NetworkSnapshotDocument commitDocument = selectCommitDocument(owner, networkId, command);
        validateAgentsExist(owner, commitDocument.agents());
        validateRelationships(owner, commitDocument.agents(), commitDocument.relationships());
        NetworkSnapshotDocument normalized = new NetworkSnapshotDocument(
                commitDocument.agents(), normalizeRelationships(commitDocument.relationships()));

        int nextVersion = latestVersion + 1;
        Instant now = Instant.now();

        NetworkVersionView version = new NetworkVersionView(
                networkId, nextVersion, command.description(), actorContext.currentActorId(), now);
        NetworkVersionView savedVersion = networkStore.createVersion(owner, version);

        saveSnapshot(owner, networkId, nextVersion, normalized, now);
        try {
            ssePublisher.publishEvent(owner, networkId, "version.deployed", savedVersion);
        } catch (Exception ignored) {
        }
        return savedVersion;
    }

    public String getDraftSnapshotJson(String networkId) {
        String owner = actorContext.currentActorId();
        requireNetwork(owner, networkId);
        NetworkSnapshotDocument draft = loadDraftSnapshot(owner, networkId);
        try {
            return objectMapper.writeValueAsString(draft);
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize draft snapshot", ex);
        }
    }

    @Transactional(readOnly = true)
    public List<NetworkVersionView> listVersions(String networkId) {
        String owner = actorContext.currentActorId();
        return networkStore.listVersions(owner, networkId);
    }

    @Transactional(readOnly = true)
    public Optional<NetworkVersionView> getVersion(String networkId, int versionNumber) {
        String owner = actorContext.currentActorId();
        return networkStore.findVersion(owner, networkId, versionNumber);
    }

    private NetworkView requireNetwork(String owner, String networkId) {
        return networkStore
                .findByOwnerIdAndNetworkId(owner, networkId)
                .orElseThrow(() -> new NotFoundException("Network not found"));
    }

    private void validateAgentsExist(String owner, List<AgentSnapshot> agents) {
        List<String> missing = agents.stream()
                .map(AgentSnapshot::agentId)
                .filter(agentId ->
                        agentStore.findByOwnerIdAndAgentId(owner, agentId).isEmpty())
                .toList();
        if (!missing.isEmpty()) {
            throw new NotFoundException("Missing agents: " + String.join(",", missing));
        }
    }

    private void validateRelationships(
            String owner, List<AgentSnapshot> agents, List<RelationshipSnapshot> relationships) {
        java.util.Set<String> deployedAgentIds =
                agents.stream().map(AgentSnapshot::agentId).collect(java.util.stream.Collectors.toSet());
        java.util.Set<String> directedKeys = new java.util.HashSet<>();
        for (RelationshipSnapshot relationship : relationships) {
            String sourceAgentId = requireText(relationship.sourceAgentId(), "sourceAgentId");
            String targetAgentId = requireText(relationship.targetAgentId(), "targetAgentId");
            if (sourceAgentId.equals(targetAgentId)) {
                throw new BadRequestException("Self-relationships are not allowed");
            }
            if (!deployedAgentIds.contains(sourceAgentId) || !deployedAgentIds.contains(targetAgentId)) {
                throw new ConflictException("Relationships require both agents to be deployed in the network");
            }
            String directedKey = sourceAgentId + "->" + targetAgentId;
            if (!directedKeys.add(directedKey)) {
                throw new ConflictException("Duplicate relationship edge in request");
            }
        }
    }

    private void ensureAgentsAreDeployed(
            String owner, NetworkSnapshotDocument draft, String sourceAgentId, String targetAgentId) {
        List<String> deployedAgentIds = draft.agents() == null
                ? List.of()
                : draft.agents().stream().map(AgentSnapshot::agentId).toList();
        if (agentStore.findByOwnerIdAndAgentId(owner, sourceAgentId).isEmpty()
                || agentStore.findByOwnerIdAndAgentId(owner, targetAgentId).isEmpty()) {
            throw new NotFoundException("Agent not found");
        }
        if (!deployedAgentIds.contains(sourceAgentId) || !deployedAgentIds.contains(targetAgentId)) {
            throw new ConflictException("Agents must be deployed in the network");
        }
    }

    private NetworkSnapshotDocument selectCommitDocument(
            String owner, String networkId, NetworkVersionCommand command) {
        List<AgentSnapshot> agents = command.agents() == null ? List.of() : command.agents();
        List<RelationshipSnapshot> relationships =
                command.relationships() == null ? List.of() : command.relationships();
        boolean requestHasSnapshot = !agents.isEmpty() || !relationships.isEmpty();
        if (requestHasSnapshot) {
            NetworkSnapshotDocument requestDoc = new NetworkSnapshotDocument(agents, relationships);
            saveDraftSnapshot(owner, networkId, requestDoc);
            return requestDoc;
        }
        return loadDraftSnapshot(owner, networkId);
    }

    private NetworkSnapshotDocument loadDraftSnapshot(String owner, String networkId) {
        return snapshotStore
                .findSnapshot(owner, networkId, DRAFT_VERSION_NUMBER)
                .map(snapshot -> readSnapshot(snapshot.snapshotJson()))
                .orElseGet(this::emptySnapshotDocument);
    }

    private void saveDraftSnapshot(String owner, String networkId, NetworkSnapshotDocument document) {
        saveSnapshot(owner, networkId, DRAFT_VERSION_NUMBER, document, Instant.now());
    }

    private void saveSnapshot(
            String owner, String networkId, int versionNumber, NetworkSnapshotDocument document, Instant createdAt) {
        try {
            NetworkSnapshotRecord record = new NetworkSnapshotRecord(
                    networkId, versionNumber, objectMapper.writeValueAsString(document), createdAt);
            snapshotStore.saveSnapshot(owner, record);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize network snapshot", ex);
        }
    }

    private NetworkSnapshotDocument readSnapshot(String snapshotJson) {
        if (snapshotJson == null || snapshotJson.isBlank()) {
            return emptySnapshotDocument();
        }
        try {
            NetworkSnapshotDocument document = objectMapper.readValue(snapshotJson, NetworkSnapshotDocument.class);
            List<AgentSnapshot> agents = document.agents() == null ? List.of() : document.agents();
            List<RelationshipSnapshot> relationships =
                    document.relationships() == null ? List.of() : document.relationships();
            return new NetworkSnapshotDocument(agents, relationships);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to read network snapshot", ex);
        }
    }

    private List<RelationshipSnapshot> normalizeRelationships(List<RelationshipSnapshot> relationships) {
        if (relationships == null || relationships.isEmpty()) {
            return List.of();
        }

        java.util.Map<String, RelationshipSnapshot> byPair = new java.util.HashMap<>();
        for (RelationshipSnapshot relationship : relationships) {
            byPair.put(pairKey(relationship.sourceAgentId(), relationship.targetAgentId()), relationship);
        }

        List<RelationshipSnapshot> normalized = new ArrayList<>();
        java.util.Set<String> processedPairs = new java.util.HashSet<>();
        for (RelationshipSnapshot relationship : relationships) {
            String pair = pairKey(relationship.sourceAgentId(), relationship.targetAgentId());
            if (!processedPairs.add(pair)) {
                continue;
            }

            String reverseKey = pairKey(relationship.targetAgentId(), relationship.sourceAgentId());
            RelationshipSnapshot reverse = byPair.get(reverseKey);
            if (reverse == null || reverse == relationship) {
                String forwardId = hasText(relationship.relationshipId())
                        ? relationship.relationshipId()
                        : UUID.randomUUID().toString();
                String reverseId = UUID.randomUUID().toString();
                String mirrorGroupId = firstText(
                        relationship.mirrorGroupId(), UUID.randomUUID().toString());
                normalized.add(new RelationshipSnapshot(
                        forwardId,
                        mirrorGroupId,
                        reverseId,
                        relationship.sourceAgentId(),
                        relationship.targetAgentId(),
                        relationship.weight(),
                        relationship.trustValue(),
                        relationship.relationshipType(),
                        relationship.transmissionMode()));
                normalized.add(new RelationshipSnapshot(
                        reverseId,
                        mirrorGroupId,
                        forwardId,
                        relationship.targetAgentId(),
                        relationship.sourceAgentId(),
                        relationship.weight(),
                        relationship.trustValue(),
                        relationship.relationshipType(),
                        relationship.transmissionMode()));
                continue;
            }

            String mirrorGroupId = firstText(
                    relationship.mirrorGroupId(),
                    reverse.mirrorGroupId(),
                    UUID.randomUUID().toString());
            String forwardId = hasText(relationship.relationshipId())
                    ? relationship.relationshipId()
                    : UUID.randomUUID().toString();
            String reverseId = hasText(reverse.relationshipId())
                    ? reverse.relationshipId()
                    : UUID.randomUUID().toString();
            normalized.add(new RelationshipSnapshot(
                    forwardId,
                    mirrorGroupId,
                    reverseId,
                    relationship.sourceAgentId(),
                    relationship.targetAgentId(),
                    relationship.weight(),
                    relationship.trustValue(),
                    relationship.relationshipType(),
                    relationship.transmissionMode()));
            normalized.add(new RelationshipSnapshot(
                    reverseId,
                    mirrorGroupId,
                    forwardId,
                    reverse.sourceAgentId(),
                    reverse.targetAgentId(),
                    reverse.weight(),
                    reverse.trustValue(),
                    reverse.relationshipType(),
                    reverse.transmissionMode()));
        }
        return normalized;
    }

    private RelationshipView toView(RelationshipSnapshot relationship) {
        return new RelationshipView(
                relationship.relationshipId(),
                relationship.mirrorGroupId(),
                relationship.reverseRelationshipId(),
                relationship.sourceAgentId(),
                relationship.targetAgentId(),
                relationship.weight(),
                relationship.trustValue(),
                relationship.relationshipType(),
                relationship.transmissionMode());
    }

    private boolean isMirrored(RelationshipSnapshot relationship) {
        return hasText(relationship.mirrorGroupId()) || hasText(relationship.reverseRelationshipId());
    }

    private String pairKey(String sourceAgentId, String targetAgentId) {
        return sourceAgentId.compareTo(targetAgentId) <= 0
                ? sourceAgentId + "|" + targetAgentId
                : targetAgentId + "|" + sourceAgentId;
    }

    private boolean sameRelationshipId(RelationshipSnapshot relationship, String relationshipId) {
        return hasText(relationshipId) && relationshipId.equals(relationship.relationshipId());
    }

    private String requireText(String value, String fieldName) {
        if (!hasText(value)) {
            throw new BadRequestException(fieldName + " is required");
        }
        return value;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private AgentSnapshot toSnapshot(AgentView agent) {
        return new AgentSnapshot(
                agent.agentId(),
                agent.nickname(),
                agent.role(),
                agent.bias(),
                agent.stubbornness(),
                agent.susceptibility(),
                agent.suspiciousness());
    }

    private NetworkSnapshotDocument emptySnapshotDocument() {
        return new NetworkSnapshotDocument(List.of(), List.of());
    }

    private record NetworkSnapshotDocument(List<AgentSnapshot> agents, List<RelationshipSnapshot> relationships) {}
}
