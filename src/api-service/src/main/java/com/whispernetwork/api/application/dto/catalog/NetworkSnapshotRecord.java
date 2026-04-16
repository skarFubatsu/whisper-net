package com.whispernetwork.api.application.dto.catalog;

import java.time.Instant;

public record NetworkSnapshotRecord(String networkId, int versionNumber, String snapshotJson, Instant createdAt) {}
