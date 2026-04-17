package com.whispernetwork.api.application.security;

/**
 * Provides the current actor identity for authorization checks.
 */
public interface ActorContext {

    /**
     * Returns the current actor identifier.
     */
    String currentActorId();
}
