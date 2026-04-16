package com.whispernetwork.api.application.dto.catalog;

public record AgentCommand(
        String agentId,
        String nickname,
        String role,
        Double bias,
        Double stubbornness,
        Double susceptibility,
        Double suspiciousness) {}
