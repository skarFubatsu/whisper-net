package com.whispernetwork.api.application.dto;

/**
 * Application command for requesting a simulation start.
 */
public record StartSimulationCommand(
        String networkId,
        int networkVersionNumber,
        String ownerId,
        String actorId,
        String clientRequestId,
        int requestedTicks) {}
