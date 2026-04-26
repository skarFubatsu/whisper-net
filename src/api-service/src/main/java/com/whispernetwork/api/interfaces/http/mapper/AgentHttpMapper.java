package com.whispernetwork.api.interfaces.http.mapper;

import com.whispernetwork.api.application.dto.catalog.AgentCommand;
import com.whispernetwork.api.application.dto.catalog.AgentView;
import com.whispernetwork.api.application.dto.catalog.PageResult;
import com.whispernetwork.api.interfaces.http.dto.AgentPageResponse;
import com.whispernetwork.api.interfaces.http.dto.AgentRequest;
import com.whispernetwork.api.interfaces.http.dto.AgentResponse;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class AgentHttpMapper {
    public AgentCommand toCommand(AgentRequest request) {
        return new AgentCommand(
                request.agentId(),
                request.nickname(),
                request.role(),
                request.bias(),
                request.stubbornness(),
                request.susceptibility(),
                request.suspiciousness());
    }

    public AgentResponse toResponse(AgentView view) {
        return new AgentResponse(
                view.agentId(),
                view.nickname(),
                view.role(),
                view.bias(),
                view.stubbornness(),
                view.susceptibility(),
                view.suspiciousness(),
                view.createdAt(),
                view.updatedAt(),
                view.createdBy(),
                view.updatedBy());
    }

    public AgentPageResponse toPageResponse(PageResult<AgentView> page) {
        List<AgentResponse> items = page.items().stream().map(this::toResponse).collect(Collectors.toList());
        return new AgentPageResponse(items, page.page(), page.size(), page.total());
    }
}
