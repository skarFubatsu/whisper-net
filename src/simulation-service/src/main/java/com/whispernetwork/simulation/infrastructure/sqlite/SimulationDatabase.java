package com.whispernetwork.simulation.infrastructure.sqlite;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.flywaydb.core.Flyway;

/**
 * SQLite database bootstrapper that runs Flyway migrations on startup.
 */
public final class SimulationDatabase {
  private static final String DEFAULT_DB_URL = "jdbc:sqlite::memory:";

  private final String jdbcUrl;

  /**
   * Creates a database bootstrapper configured from environment.
   */
  public SimulationDatabase() {
    this.jdbcUrl = normalizeJdbcUrl(readEnv("SIMULATION_DB_URL", DEFAULT_DB_URL));
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

  private static String readEnv(String key, String fallback) {
    String value = System.getenv(key);
    return value == null || value.isBlank() ? fallback : value;
  }

  private static String normalizeJdbcUrl(String configured) {
    if (configured.startsWith("jdbc:sqlite:")) {
      return configured;
    }
    return "jdbc:sqlite:" + configured;
  }
}
