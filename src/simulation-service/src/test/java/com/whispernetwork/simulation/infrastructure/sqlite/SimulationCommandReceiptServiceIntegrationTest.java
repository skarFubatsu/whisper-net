package com.whispernetwork.simulation.infrastructure.sqlite;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SimulationCommandReceiptService")
class SimulationCommandReceiptServiceIntegrationTest {

    @Nested
    @DisplayName("Marking")
    class MarkingTests {

        @Test
        void shouldMarkNewEventIdAndRejectDuplicate() {
            String jdbcUrl = newTempFileDbUrl();
            migrateSchema(jdbcUrl);

            SimulationCommandReceiptService receiptService = new SimulationCommandReceiptService(jdbcUrl);

            assertTrue(receiptService.markReceivedIfNew("event-1"));
            assertFalse(receiptService.markReceivedIfNew("event-1"));
        }
    }

    @Nested
    @DisplayName("BlankIds")
    class BlankIdsTests {

        @Test
        void shouldAllowBlankEventIds() {
            String jdbcUrl = newTempFileDbUrl();
            migrateSchema(jdbcUrl);

            SimulationCommandReceiptService receiptService = new SimulationCommandReceiptService(jdbcUrl);

            assertTrue(receiptService.markReceivedIfNew(""));
            assertTrue(receiptService.markReceivedIfNew("   "));
            assertTrue(receiptService.markReceivedIfNew(null));
        }
    }

    private static String newTempFileDbUrl() {
        try {
            Path file = Files.createTempFile("command-receipts-", ".db");
            file.toFile().deleteOnExit();
            return "jdbc:sqlite:" + file;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to create temp db file", ex);
        }
    }

    private static void migrateSchema(String jdbcUrl) {
        Flyway.configure()
                .dataSource(jdbcUrl, null, null)
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }
}
