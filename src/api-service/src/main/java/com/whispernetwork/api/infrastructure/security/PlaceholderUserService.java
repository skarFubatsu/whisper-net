package com.whispernetwork.api.infrastructure.security;

import com.whispernetwork.api.infrastructure.persistence.entity.UserAccountEntity;
import com.whispernetwork.api.infrastructure.persistence.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ensures the dev placeholder user exists and exposes a security principal.
 */
@Service
public class PlaceholderUserService {
    private final UserAccountRepository userAccountRepository;
    private final PlaceholderUserProperties properties;

    public PlaceholderUserService(UserAccountRepository userAccountRepository, PlaceholderUserProperties properties) {
        this.userAccountRepository = userAccountRepository;
        this.properties = properties;
    }

    @Transactional
    public UserAccountEntity getOrCreateUser() {
        String userId = properties.getUserId();
        return userAccountRepository
                .findById(userId)
                .orElseGet(() -> userAccountRepository.save(new UserAccountEntity(
                        userId, properties.getDisplayName(), properties.getEmail(), properties.getRole())));
    }

    @Transactional(readOnly = true)
    public PlaceholderUserPrincipal loadPrincipal() {
        UserAccountEntity account = getOrCreateUser();
        return new PlaceholderUserPrincipal(
                account.getUserId(), account.getDisplayName(), account.getEmail(), account.getRole());
    }
}
