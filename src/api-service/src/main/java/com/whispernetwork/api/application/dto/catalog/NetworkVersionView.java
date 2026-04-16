package com.whispernetwork.api.application.dto.catalog;

import java.time.Instant;

public record NetworkVersionView(
        String networkId, int versionNumber, String description, String createdBy, Instant createdAt) {}
