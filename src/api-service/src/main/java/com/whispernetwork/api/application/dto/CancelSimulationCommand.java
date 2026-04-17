package com.whispernetwork.api.application.dto;

/**
 * Application command for requesting a simulation cancellation.
 */
public record CancelSimulationCommand(String ownerId, String actorId, String clientRequestId) {}
