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
      this.jdbcUrl = DEFAULT_DB_URL;
      LOGGER.warning("SIMULATION_DB_URL is not set. Falling back to in-memory SQLite: " + jdbcUrl);
      return;
    }
    this.jdbcUrl = normalizeJdbcUrl(configuredUrl);
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
}
