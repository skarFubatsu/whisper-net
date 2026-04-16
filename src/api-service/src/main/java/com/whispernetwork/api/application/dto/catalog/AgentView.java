package com.whispernetwork.api.application.dto.catalog;

import java.time.Instant;

public record AgentView(
        String agentId,
        String nickname,
        String role,
        double bias,
        double stubbornness,
        double susceptibility,
        double suspiciousness,
        Instant createdAt,
        Instant updatedAt,
        String createdBy,
        String updatedBy) {}
