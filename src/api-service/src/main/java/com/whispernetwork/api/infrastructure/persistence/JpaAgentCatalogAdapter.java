package com.whispernetwork.api.infrastructure.persistence;

import com.whispernetwork.api.application.dto.catalog.AgentView;
import com.whispernetwork.api.application.dto.catalog.PageResult;
import com.whispernetwork.api.application.ports.out.AgentCatalogPort;
import com.whispernetwork.api.infrastructure.persistence.entity.AgentEntity;
import com.whispernetwork.api.infrastructure.persistence.repository.AgentJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class JpaAgentCatalogAdapter implements AgentCatalogPort {
    private final AgentJpaRepository repo;

    public JpaAgentCatalogAdapter(AgentJpaRepository repo) {
        this.repo = repo;
    }

    @Override
    public AgentView create(String ownerId, AgentView agent) {
        AgentEntity entity = toEntity(ownerId, agent, new AgentEntity());
        return toView(repo.save(entity));
    }

    @Override
    public AgentView update(String ownerId, AgentView agent) {
        AgentEntity entity = repo.findByOwnerIdAndAgentId(ownerId, agent.agentId())
                .orElseThrow(() -> new IllegalStateException("Agent not found for update: " + agent.agentId()));
        return toView(repo.save(toEntity(ownerId, agent, entity)));
    }

    @Override
    public Optional<AgentView> findByOwnerIdAndAgentId(String ownerId, String agentId) {
        return repo.findByOwnerIdAndAgentId(ownerId, agentId).map(this::toView);
    }

    @Override
    public List<AgentView> findByOwnerId(String ownerId) {
        return repo.findByOwnerId(ownerId).stream().map(this::toView).collect(Collectors.toList());
    }

    @Override
    public PageResult<AgentView> findPageByOwnerId(String ownerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return toPageResult(repo.findByOwnerId(ownerId, pageable));
    }

    @Override
    public PageResult<AgentView> findPageByOwnerIdAndAgentIdIn(
            String ownerId, List<String> agentIds, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return toPageResult(repo.findByOwnerIdAndAgentIdIn(ownerId, agentIds, pageable));
    }

    @Override
    public PageResult<AgentView> findPageByOwnerIdAndAgentIdNotIn(
            String ownerId, List<String> agentIds, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return toPageResult(repo.findByOwnerIdAndAgentIdNotIn(ownerId, agentIds, pageable));
    }

    private PageResult<AgentView> toPageResult(Page<AgentEntity> pageResult) {
        List<AgentView> items = pageResult.stream().map(this::toView).collect(Collectors.toList());
        return new PageResult<>(items, pageResult.getNumber(), pageResult.getSize(), pageResult.getTotalElements());
    }

    private AgentEntity toEntity(String ownerId, AgentView view, AgentEntity entity) {
        entity.setOwnerId(ownerId);
        entity.setAgentId(view.agentId());
        entity.setNickname(view.nickname());
        entity.setRole(view.role());
        entity.setBias(view.bias());
        entity.setStubbornness(view.stubbornness());
        entity.setSusceptibility(view.susceptibility());
        entity.setSuspiciousness(view.suspiciousness());
        entity.setCreatedAt(view.createdAt());
        entity.setCreatedBy(view.createdBy());
        entity.setUpdatedAt(view.updatedAt());
        entity.setUpdatedBy(view.updatedBy());
        return entity;
    }

    private AgentView toView(AgentEntity entity) {
        return new AgentView(
                entity.getAgentId(),
                entity.getNickname(),
                entity.getRole(),
                entity.getBias(),
                entity.getStubbornness(),
                entity.getSusceptibility(),
                entity.getSuspiciousness(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getCreatedBy(),
                entity.getUpdatedBy());
    }
}
