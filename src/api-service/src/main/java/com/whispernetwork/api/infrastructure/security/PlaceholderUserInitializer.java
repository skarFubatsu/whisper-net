package com.whispernetwork.api.infrastructure.security;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Ensures the placeholder user exists in dev/local profiles.
 */
@Component
@Profile({"dev", "local"})
public class PlaceholderUserInitializer {
    private final PlaceholderUserProperties properties;
    private final PlaceholderUserService placeholderUserService;

    public PlaceholderUserInitializer(
            PlaceholderUserProperties properties, PlaceholderUserService placeholderUserService) {
        this.properties = properties;
        this.placeholderUserService = placeholderUserService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ensurePlaceholderUser() {
        if (properties.isEnabled()) {
            placeholderUserService.getOrCreateUser();
        }
    }
}
