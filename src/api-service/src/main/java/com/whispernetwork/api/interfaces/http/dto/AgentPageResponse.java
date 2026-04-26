package com.whispernetwork.api.interfaces.http.dto;

import java.util.List;

/**
 * Paginated response wrapper for agent listing endpoints.
 */
public record AgentPageResponse(List<AgentResponse> items, int page, int size, long total) {}
