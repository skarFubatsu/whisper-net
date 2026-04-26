package com.whispernetwork.api.interfaces.http.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AgentSnapshotRequest(
        @NotBlank String agentId,
        @NotBlank String nickname,
        String role,
        @NotNull Double bias,
        @NotNull Double stubbornness,
        @NotNull Double susceptibility,
        @NotNull Double suspiciousness) {}
