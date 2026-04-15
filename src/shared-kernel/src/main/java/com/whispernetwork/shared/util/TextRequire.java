package com.whispernetwork.shared.util;

/**
 * Shared text validation helpers for command and model construction.
 */
public final class TextRequire {

    private TextRequire() {}

    /**
     * Ensures a required text value is present.
     */
    public static void nonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }
}
