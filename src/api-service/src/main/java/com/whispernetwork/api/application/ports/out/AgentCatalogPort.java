package com.whispernetwork.api.application.ports.out;

import com.whispernetwork.api.application.dto.catalog.AgentView;
import com.whispernetwork.api.application.dto.catalog.PageResult;
import java.util.List;
import java.util.Optional;

public interface AgentCatalogPort {
    AgentView create(String ownerId, AgentView agent);

    AgentView update(String ownerId, AgentView agent);

    Optional<AgentView> findByOwnerIdAndAgentId(String ownerId, String agentId);

    List<AgentView> findByOwnerId(String ownerId);

    PageResult<AgentView> findPageByOwnerId(String ownerId, int page, int size);

    PageResult<AgentView> findPageByOwnerIdAndAgentIdIn(String ownerId, List<String> agentIds, int page, int size);

    PageResult<AgentView> findPageByOwnerIdAndAgentIdNotIn(String ownerId, List<String> agentIds, int page, int size);
}
