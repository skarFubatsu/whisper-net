package com.whispernetwork.api.application.dto.catalog;

public record RelationshipView(
        String relationshipId,
        String mirrorGroupId,
        String reverseRelationshipId,
        String sourceAgentId,
        String targetAgentId,
        Double weight,
        Double trustValue,
        String relationshipType,
        String transmissionMode) {}
