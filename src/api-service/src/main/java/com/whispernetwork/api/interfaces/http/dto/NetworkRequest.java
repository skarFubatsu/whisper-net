package com.whispernetwork.api.interfaces.http.dto;

import jakarta.validation.constraints.NotBlank;

public record NetworkRequest(@NotBlank String name, String networkId) {}
