package com.whispernetwork.api.interfaces.http.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RelationshipSnapshotRequest(
        String relationshipId,
        String mirrorGroupId,
        String reverseRelationshipId,
        @NotBlank String sourceAgentId,
        @NotBlank String targetAgentId,
        @NotNull Double weight,
        @NotNull Double trustValue,
        @NotBlank String relationshipType,
        @NotBlank String transmissionMode) {}
