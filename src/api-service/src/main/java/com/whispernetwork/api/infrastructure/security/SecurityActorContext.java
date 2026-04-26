package com.whispernetwork.api.infrastructure.security;

import com.whispernetwork.api.application.security.ActorContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Resolves the current actor from Spring Security.
 */
@Component
public class SecurityActorContext implements ActorContext {
    private final PlaceholderUserProperties properties;

    public SecurityActorContext(PlaceholderUserProperties properties) {
        this.properties = properties;
    }

    @Override
    public String currentActorId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetails userDetails) {
                return userDetails.getUsername();
            }
            if (principal instanceof String stringPrincipal && !stringPrincipal.isBlank()) {
                return stringPrincipal;
            }
        }

        if (properties.isEnabled()) {
            return properties.getUserId();
        }

        throw new IllegalStateException("No authenticated actor available.");
    }
}
