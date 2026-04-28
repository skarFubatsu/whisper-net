package com.whispernetwork.api.interfaces.http.dto;

public record RelationshipResponse(
        String relationshipId,
        String mirrorGroupId,
        String reverseRelationshipId,
        String sourceAgentId,
        String targetAgentId,
        Double weight,
        Double trustValue,
        String relationshipType,
        String transmissionMode) {}
