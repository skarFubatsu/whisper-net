package com.whispernetwork.api.application.dto.catalog;

import java.time.Instant;

public record NetworkView(String networkId, String name, Instant createdAt, Instant updatedAt) {}
