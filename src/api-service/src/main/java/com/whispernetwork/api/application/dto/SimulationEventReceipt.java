package com.whispernetwork.api.application.dto;

import java.time.Instant;

public record SimulationEventReceipt(String eventId, Instant receivedAt) {}
