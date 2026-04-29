package com.whispernetwork.api.interfaces.http.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * HTTP payload for simulation start requests.
 */
public record StartSimulationRequest(
        @NotBlank String networkId,
        @NotNull @Min(1) Integer networkVersionNumber,
        @NotBlank String ownerId,
        @NotBlank String actorId,
        @NotBlank String clientRequestId,
        @NotNull @Min(1) @Max(100_000) Integer requestedTicks) {}
