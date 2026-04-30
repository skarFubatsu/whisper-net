package com.whispernetwork.api.application.services.catalog;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("PaginationValidator")
class PaginationValidatorTest {

    @Nested
    @DisplayName("validate(int page, int size)")
    class ValidateTests {
        @Test
        @DisplayName("accepts valid pagination parameters")
        void acceptsValidParameters() {
            assertDoesNotThrow(() -> PaginationValidator.validate(0, 50));
            assertDoesNotThrow(() -> PaginationValidator.validate(1, 100));
            assertDoesNotThrow(() -> PaginationValidator.validate(10, 25));
        }

        @Test
        @DisplayName("rejects negative page")
        void rejectsNegativePage() {
            IllegalArgumentException ex =
                    assertThrows(IllegalArgumentException.class, () -> PaginationValidator.validate(-1, 50));
            assertTrue(ex.getMessage().contains("page"));
        }

        @Test
        @DisplayName("rejects size less than 1")
        void rejectsSizeLessThanOne() {
            IllegalArgumentException ex =
                    assertThrows(IllegalArgumentException.class, () -> PaginationValidator.validate(0, 0));
            assertTrue(ex.getMessage().contains("size"));
        }

        @Test
        @DisplayName("rejects size greater than 100")
        void rejectsSizeGreaterThan100() {
            IllegalArgumentException ex =
                    assertThrows(IllegalArgumentException.class, () -> PaginationValidator.validate(0, 101));
            assertTrue(ex.getMessage().contains("size"));
        }

        @Test
        @DisplayName("enforces maximum page size of 100")
        void enforcesMaxPageSizeOf100() {
            assertDoesNotThrow(() -> PaginationValidator.validate(0, 100));
            assertThrows(IllegalArgumentException.class, () -> PaginationValidator.validate(0, 101));
        }
    }
}
