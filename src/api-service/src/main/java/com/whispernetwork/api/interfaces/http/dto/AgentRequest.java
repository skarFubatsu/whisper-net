package com.whispernetwork.api.interfaces.http.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * HTTP payload for creating or updating an agent.
 */
public record AgentRequest(
        String agentId,
        @NotBlank String nickname,
        String role,
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double bias,
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double stubbornness,
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double susceptibility,
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double suspiciousness) {}
