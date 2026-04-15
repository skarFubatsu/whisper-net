package com.whispernetwork.simulation.infrastructure.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class SimulationDatabaseTest {

    @Test
    void shouldUseSharedInMemoryUrlAsDefaultFallback() throws Exception {
        Field defaultDbUrlField = SimulationDatabase.class.getDeclaredField("DEFAULT_DB_URL");
        defaultDbUrlField.setAccessible(true);

        String defaultDbUrl = (String) defaultDbUrlField.get(null);

        assertEquals("jdbc:sqlite:file:simulation_mem?mode=memory&cache=shared", defaultDbUrl);
    }

    @Test
    void shouldNormalizeNonJdbcSqlitePathToJdbcForm() throws Exception {
        Method normalizeMethod = SimulationDatabase.class.getDeclaredMethod("normalizeJdbcUrl", String.class);
        normalizeMethod.setAccessible(true);

        String normalized = (String) normalizeMethod.invoke(null, "var/data/simulation.db");

        assertEquals("jdbc:sqlite:var/data/simulation.db", normalized);
    }

    @Test
    void shouldKeepJdbcSqliteUrlUnchanged() throws Exception {
        Method normalizeMethod = SimulationDatabase.class.getDeclaredMethod("normalizeJdbcUrl", String.class);
        normalizeMethod.setAccessible(true);

        String original = "jdbc:sqlite:file:integration_test?mode=memory&cache=shared";
        String normalized = (String) normalizeMethod.invoke(null, original);

        assertEquals(original, normalized);
    }
}
