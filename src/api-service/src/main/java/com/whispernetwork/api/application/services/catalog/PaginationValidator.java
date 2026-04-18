package com.whispernetwork.api.application.services.catalog;

final class PaginationValidator {
    static final int MAX_PAGE_SIZE = 100;

    private PaginationValidator() {}

    static void validate(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (size <= 0 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }
    }
}
