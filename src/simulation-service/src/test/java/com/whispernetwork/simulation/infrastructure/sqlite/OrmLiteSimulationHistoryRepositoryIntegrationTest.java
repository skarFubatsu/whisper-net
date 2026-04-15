package com.whispernetwork.simulation.infrastructure.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.whispernetwork.shared.dto.SimulationHistoryEntry;
import com.whispernetwork.shared.dto.TimelineQuery;
import com.whispernetwork.shared.dto.TimelineWindow;
import com.whispernetwork.simulation.application.model.SimulationEvent;
import com.whispernetwork.simulation.core.engine.AgentOpinionUpdate;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

class OrmLiteSimulationHistoryRepositoryIntegrationTest {

    @Test
    void shouldRecordEventsAndFilterByTypeAndRun() {
        String jdbcUrl = newTempFileDbUrl();
        migrateSchema(jdbcUrl);

        String runId = UUID.randomUUID().toString();
        Instant t1 = Instant.ofEpochMilli(1_000L);
        Instant t2 = Instant.ofEpochMilli(2_000L);
        Instant t3 = Instant.ofEpochMilli(3_000L);

        SimulationEvent started = new SimulationEvent.SimulationStarted(
                runId, "network-a", 1, "actor-a", "req-a", "owner-a", "req-a", t1);

        SimulationEvent tickCompleted = new SimulationEvent.SimulationTickCompleted(
                runId,
                "network-a",
                1,
                1,
                List.of(new AgentOpinionUpdate("agent-1", 0.1, 0.3, 0.2, false, null, false, List.of("agent-2"))),
                "owner-a",
                "req-a",
                t2);

        SimulationEvent failed =
                new SimulationEvent.SimulationFailed(runId, "network-a", "boom", "owner-a", "req-a", t3);

        try (OrmLiteSimulationHistoryRepository repository = new OrmLiteSimulationHistoryRepository(jdbcUrl)) {
            repository.record(started);
            repository.record(tickCompleted);
            repository.record(failed);

            TimelineQuery tickOnlyQuery = new TimelineQuery(
                    runId, "network-a", List.of("SIMULATION_TICK_COMPLETED"), new TimelineWindow(null, null, 50, 0L));

            List<SimulationHistoryEntry> tickOnly = repository.list(tickOnlyQuery);
            assertEquals(1, tickOnly.size());
            assertEquals("SIMULATION_TICK_COMPLETED", tickOnly.get(0).eventType());
            assertEquals(1, tickOnly.get(0).tickNumber());
            assertEquals(1, tickOnly.get(0).updatedAgents());

            TimelineQuery orderedQuery =
                    new TimelineQuery(runId, "network-a", List.of(), new TimelineWindow(null, null, 50, 0L));
            List<SimulationHistoryEntry> ordered = repository.list(orderedQuery);

            assertEquals(3, ordered.size());
            assertEquals("SIMULATION_FAILED", ordered.get(0).eventType());
            assertEquals("SIMULATION_TICK_COMPLETED", ordered.get(1).eventType());
            assertEquals("SIMULATION_STARTED", ordered.get(2).eventType());
        }
    }

    @Test
    void shouldApplyTimeWindowAndPagination() {
        String jdbcUrl = newTempFileDbUrl();
        migrateSchema(jdbcUrl);

        String runId = UUID.randomUUID().toString();

        try (OrmLiteSimulationHistoryRepository repository = new OrmLiteSimulationHistoryRepository(jdbcUrl)) {
            repository.record(new SimulationEvent.SimulationStarted(
                    runId, "network-a", 1, "actor-a", "req-a", "owner-a", "req-a", Instant.ofEpochMilli(1_000L)));

            repository.record(new SimulationEvent.SimulationTickCompleted(
                    runId, "network-a", 1, 0, List.of(), "owner-a", "req-a", Instant.ofEpochMilli(2_000L)));

            repository.record(new SimulationEvent.SimulationCompleted(
                    runId, "network-a", 1, "owner-a", "req-a", Instant.ofEpochMilli(3_000L)));

            TimelineQuery windowed =
                    new TimelineQuery(runId, "network-a", List.of(), new TimelineWindow(1_500L, 3_000L, 10, 0L));

            List<SimulationHistoryEntry> inWindow = repository.list(windowed);
            assertEquals(2, inWindow.size());
            assertEquals("SIMULATION_COMPLETED", inWindow.get(0).eventType());
            assertEquals("SIMULATION_TICK_COMPLETED", inWindow.get(1).eventType());

            TimelineQuery paged =
                    new TimelineQuery(runId, "network-a", List.of(), new TimelineWindow(null, null, 1, 1L));

            List<SimulationHistoryEntry> page = repository.list(paged);
            assertEquals(1, page.size());
            assertEquals("SIMULATION_TICK_COMPLETED", page.get(0).eventType());
        }
    }

    private static String newTempFileDbUrl() {
        try {
            Path file = Files.createTempFile("history-repo-", ".db");
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
