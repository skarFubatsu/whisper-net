package com.whispernetwork.api.application.services.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whispernetwork.api.application.dto.catalog.AgentCommand;
import com.whispernetwork.api.application.dto.catalog.AgentView;
import com.whispernetwork.api.application.dto.catalog.NetworkSnapshotRecord;
import com.whispernetwork.api.application.dto.catalog.PageResult;
import com.whispernetwork.api.application.ports.out.AgentCatalogPort;
import com.whispernetwork.api.application.ports.out.NetworkSnapshotPort;
import com.whispernetwork.api.application.security.ActorContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgentCatalogApplicationService {
    private final AgentCatalogPort agentStore;
    private final NetworkSnapshotPort snapshotStore;
    private final ActorContext actorContext;
    private final ObjectMapper objectMapper;

    public AgentCatalogApplicationService(
            AgentCatalogPort agentStore,
            NetworkSnapshotPort snapshotStore,
            ActorContext actorContext,
            ObjectMapper objectMapper) {
        this.agentStore = agentStore;
        this.snapshotStore = snapshotStore;
        this.actorContext = actorContext;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AgentView createAgent(AgentCommand command) {
        String owner = actorContext.currentActorId();
        String agentId = command.agentId() == null || command.agentId().isBlank()
                ? java.util.UUID.randomUUID().toString()
                : command.agentId();
        Instant now = Instant.now();
        AgentView toCreate = new AgentView(
                agentId,
                command.nickname(),
                command.role(),
                command.bias(),
                command.stubbornness(),
                command.susceptibility(),
                command.suspiciousness(),
                now,
                now,
                owner,
                owner);
        return agentStore.create(owner, toCreate);
    }

    @Transactional
    public Optional<AgentView> updateAgent(String agentId, AgentCommand command) {
        String owner = actorContext.currentActorId();
        Optional<AgentView> found = agentStore.findByOwnerIdAndAgentId(owner, agentId);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        AgentView existing = found.get();
        AgentView updated = new AgentView(
                existing.agentId(),
                command.nickname(),
                command.role(),
                command.bias(),
                command.stubbornness(),
                command.susceptibility(),
                command.suspiciousness(),
                existing.createdAt(),
                Instant.now(),
                existing.createdBy(),
                owner);
        return Optional.of(agentStore.update(owner, updated));
    }

    @Transactional(readOnly = true)
    public Optional<AgentView> findAgent(String agentId) {
        String owner = actorContext.currentActorId();
        return agentStore.findByOwnerIdAndAgentId(owner, agentId);
    }

    @Transactional(readOnly = true)
    public List<AgentView> listAgents() {
        String owner = actorContext.currentActorId();
        return agentStore.findByOwnerId(owner);
    }

    @Transactional(readOnly = true)
    public PageResult<AgentView> listAgents(int page, int size, Boolean deployed, String networkId) {
        PaginationValidator.validate(page, size);

        String owner = actorContext.currentActorId();
        if (deployed == null) {
            return agentStore.findPageByOwnerId(owner, page, size);
        }

        if (networkId == null || networkId.isBlank()) {
            throw new IllegalArgumentException("networkId is required when deployed filter is provided");
        }

        List<String> latestSnapshotAgentIds = latestSnapshotAgentIds(owner, networkId);
        if (deployed) {
            if (latestSnapshotAgentIds.isEmpty()) {
                return new PageResult<>(List.of(), page, size, 0);
            }
            return agentStore.findPageByOwnerIdAndAgentIdIn(owner, latestSnapshotAgentIds, page, size);
        }

        if (latestSnapshotAgentIds.isEmpty()) {
            return agentStore.findPageByOwnerId(owner, page, size);
        }
        return agentStore.findPageByOwnerIdAndAgentIdNotIn(owner, latestSnapshotAgentIds, page, size);
    }

    private List<String> latestSnapshotAgentIds(String owner, String networkId) {
        Optional<NetworkSnapshotRecord> snapshotRecord = snapshotStore.findLatestSnapshot(owner, networkId);
        if (snapshotRecord.isEmpty()
                || snapshotRecord.get().snapshotJson() == null
                || snapshotRecord.get().snapshotJson().isBlank()) {
            return List.of();
        }

        try {
            JsonNode root = objectMapper.readTree(snapshotRecord.get().snapshotJson());
            JsonNode agents = root.path("agents");
            if (!agents.isArray()) {
                return List.of();
            }

            List<String> ids = new ArrayList<>();
            for (JsonNode agent : agents) {
                String id = agent.path("agentId").asText(null);
                if (id != null && !id.isBlank()) {
                    ids.add(id);
                }
            }
            return ids;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid snapshot format for network: " + networkId, ex);
        }
    }
}
