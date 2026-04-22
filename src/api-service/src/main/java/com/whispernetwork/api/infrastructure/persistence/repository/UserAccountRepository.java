package com.whispernetwork.api.infrastructure.persistence.repository;

import com.whispernetwork.api.infrastructure.persistence.entity.UserAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA repository for user accounts.
 */
public interface UserAccountRepository extends JpaRepository<UserAccountEntity, String> {}
