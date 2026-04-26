package com.whispernetwork.api.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Injects a dev placeholder user into the security context when enabled.
 */
public class PlaceholderUserFilter extends OncePerRequestFilter {
    private final PlaceholderUserService placeholderUserService;
    private final PlaceholderUserProperties properties;

    public PlaceholderUserFilter(PlaceholderUserService placeholderUserService, PlaceholderUserProperties properties) {
        this.placeholderUserService = placeholderUserService;
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (properties.isEnabled() && SecurityContextHolder.getContext().getAuthentication() == null) {
            PlaceholderUserPrincipal principal = placeholderUserService.loadPrincipal();
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
