package com.whispernetwork.api.infrastructure.security;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Prevents enabling placeholder security outside dev/local profiles.
 */
@Component
public class PlaceholderSecurityGuard {
    private static final Set<String> ALLOWED_PROFILES = new HashSet<>(Arrays.asList("dev", "local"));

    private final Environment environment;
    private final PlaceholderUserProperties properties;

    public PlaceholderSecurityGuard(Environment environment, PlaceholderUserProperties properties) {
        this.environment = environment;
        this.properties = properties;
    }

    @PostConstruct
    public void validateProfileUsage() {
        if (!properties.isEnabled()) {
            return;
        }

        boolean allowed = Arrays.stream(environment.getActiveProfiles()).anyMatch(ALLOWED_PROFILES::contains);
        if (!allowed) {
            throw new IllegalStateException("Placeholder security may only run under dev/local profiles.");
        }
    }
}
