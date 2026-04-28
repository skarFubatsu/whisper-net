package com.whispernetwork.api.interfaces.http.dto;

import java.util.List;

public record RelationshipPageResponse(List<RelationshipResponse> items, int page, int size, long total) {}
