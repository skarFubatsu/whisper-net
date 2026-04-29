package com.whispernetwork.api.interfaces.http.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * HTTP payload for simulation cancellation requests.
 */
public record CancelSimulationRequest(
        @NotBlank String ownerId, @NotBlank String actorId, @NotBlank String clientRequestId) {}
