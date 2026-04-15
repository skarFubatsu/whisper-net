package com.whispernetwork.simulation.infrastructure.sqlite;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;
import org.flywaydb.core.Flyway;

/**
 * SQLite database bootstrapper that runs Flyway migrations on startup.
 */
public final class SimulationDatabase {
    private static final Logger LOGGER = Logger.getLogger(SimulationDatabase.class.getName());
    private static final String DEFAULT_DB_URL = "jdbc:sqlite:file:simulation_mem?mode=memory&cache=shared";

    private final String jdbcUrl;

    /**
     * Creates a database bootstrapper configured from environment.
     */
    public SimulationDatabase() {
        String configuredUrl = System.getenv("SIMULATION_DB_URL");
        if (configuredUrl == null || configuredUrl.isBlank()) {
            this.jdbcUrl = applySqlitePragmas(DEFAULT_DB_URL);
            LOGGER.warning("SIMULATION_DB_URL is not set. Falling back to in-memory SQLite: " + jdbcUrl);
            return;
        }
        this.jdbcUrl = applySqlitePragmas(normalizeJdbcUrl(configuredUrl));
    }

    /**
     * Returns active JDBC URL.
     */
    public String jdbcUrl() {
        return jdbcUrl;
    }

    /**
     * Runs all pending Flyway migrations.
     */
    public void migrate() {
        ensureParentDirectory();
        Flyway.configure()
                .dataSource(jdbcUrl, null, null)
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    private void ensureParentDirectory() {
        String prefix = "jdbc:sqlite:";
        if (!jdbcUrl.startsWith(prefix)) {
            return;
        }

        String dbPath = jdbcUrl.substring(prefix.length());
        if (dbPath.equals(":memory:") || dbPath.contains("mode=memory")) {
            return;
        }

        int queryIndex = dbPath.indexOf('?');
        if (queryIndex >= 0) {
            dbPath = dbPath.substring(0, queryIndex);
        }
        if (dbPath.startsWith("file:")) {
            dbPath = dbPath.substring("file:".length());
        }

        Path path = Paths.get(dbPath).normalize();
        Path parent = path.getParent();
        if (parent == null) {
            return;
        }

        try {
            Files.createDirectories(parent);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to create SQLite data directory: " + parent, ex);
        }
    }

    private static String normalizeJdbcUrl(String configured) {
        if (configured.startsWith("jdbc:sqlite:")) {
            return configured;
        }
        return "jdbc:sqlite:" + configured;
    }

    private static String applySqlitePragmas(String jdbcUrl) {
        String base = jdbcUrl;
        String query = "";
        int queryIndex = jdbcUrl.indexOf('?');
        if (queryIndex >= 0) {
            base = jdbcUrl.substring(0, queryIndex);
            query = jdbcUrl.substring(queryIndex + 1);
        }

        boolean hasJournalMode = query.contains("journal_mode=");
        boolean hasBusyTimeout = query.contains("busy_timeout=");
        if (hasJournalMode && hasBusyTimeout) {
            return jdbcUrl;
        }

        StringBuilder updated = new StringBuilder(base);
        if (query.isBlank()) {
            updated.append('?');
        } else {
            updated.append('?').append(query);
            if (!query.endsWith("&")) {
                updated.append('&');
            }
        }

        if (!hasJournalMode) {
            updated.append("journal_mode=WAL");
        }
        if (!hasBusyTimeout) {
            if (!updated.toString().endsWith("?") && !updated.toString().endsWith("&")) {
                updated.append('&');
            }
            updated.append("busy_timeout=5000");
        }

        return updated.toString();
    }
}
