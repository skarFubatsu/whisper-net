package com.whispernetwork.api.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

/**
 * Dev-only security configuration that injects a placeholder user.
 */
@Configuration
@EnableWebSecurity
@Profile({"dev", "local"})
public class DevPlaceholderSecurityConfig {

    @Bean
    public SecurityFilterChain devSecurityFilterChain(
            HttpSecurity http, PlaceholderUserService placeholderUserService, PlaceholderUserProperties properties)
            throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {})
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .addFilterBefore(
                        new PlaceholderUserFilter(placeholderUserService, properties),
                        AnonymousAuthenticationFilter.class);

        return http.build();
    }
}
