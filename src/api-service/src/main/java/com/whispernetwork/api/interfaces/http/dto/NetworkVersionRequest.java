package com.whispernetwork.api.interfaces.http.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Request to create a new network version with an optional full graph snapshot.
 */
public record NetworkVersionRequest(
        String description,
        Integer baseVersionNumber,
        @NotNull List<AgentSnapshotRequest> agents,
        @NotNull List<RelationshipSnapshotRequest> relationships) {}
