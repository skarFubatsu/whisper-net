package com.whispernetwork.api.interfaces.http.dto;

import java.time.Instant;

public record NetworkResponse(String networkId, String name, Instant createdAt, Instant updatedAt) {}
