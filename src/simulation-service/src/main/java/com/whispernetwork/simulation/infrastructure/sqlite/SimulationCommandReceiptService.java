package com.whispernetwork.simulation.infrastructure.sqlite;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;

/**
 * Deduplicates inbound simulation commands by event id.
 */
public final class SimulationCommandReceiptService {
    private final String jdbcUrl;

    /**
     * Creates a receipt service using the provided JDBC URL.
     */
    public SimulationCommandReceiptService(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    /**
     * Marks a command event id as received.
     *
     * @return true when event id is new or blank, false when already processed.
     */
    public synchronized boolean markReceivedIfNew(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            return true;
        }

        try (Connection connection = DriverManager.getConnection(jdbcUrl);
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT OR IGNORE INTO simulation_command_receipts(event_id, received_at_epoch_millis) VALUES (?, ?)")) {
            statement.setString(1, eventId);
            statement.setLong(2, Instant.now().toEpochMilli());
            return statement.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to persist simulation command receipt", ex);
        }
    }
}
