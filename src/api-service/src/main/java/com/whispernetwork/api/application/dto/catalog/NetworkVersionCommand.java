package com.whispernetwork.api.application.dto.catalog;

import java.util.List;

public record NetworkVersionCommand(
        String description,
        Integer baseVersionNumber,
        List<AgentSnapshot> agents,
        List<RelationshipSnapshot> relationships) {}
