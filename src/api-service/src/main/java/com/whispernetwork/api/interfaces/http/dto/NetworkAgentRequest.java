package com.whispernetwork.api.interfaces.http.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Minimal request for deploying an existing agent into a network draft.
 */
public record NetworkAgentRequest(@NotBlank String agentId) {}
