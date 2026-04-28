package com.whispernetwork.api.interfaces.http.dto;

import java.time.Instant;

public record NetworkVersionResponse(
        String networkId, int versionNumber, String description, String createdBy, Instant createdAt) {}
