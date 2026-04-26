package com.whispernetwork.api.interfaces.http.dto;

import java.time.Instant;

/**
 * HTTP response model for an agent.
 */
public record AgentResponse(
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
