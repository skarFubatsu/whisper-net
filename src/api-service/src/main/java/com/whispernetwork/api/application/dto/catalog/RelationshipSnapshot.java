package com.whispernetwork.api.application.dto.catalog;

public record RelationshipSnapshot(
        String relationshipId,
        String mirrorGroupId,
        String reverseRelationshipId,
        String sourceAgentId,
        String targetAgentId,
        Double weight,
        Double trustValue,
        String relationshipType,
        String transmissionMode) {}
