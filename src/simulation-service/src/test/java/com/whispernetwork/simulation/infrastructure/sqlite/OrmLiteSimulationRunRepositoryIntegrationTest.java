package com.whispernetwork.simulation.infrastructure.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.whispernetwork.shared.dto.RunStatus;
import com.whispernetwork.simulation.application.model.IdempotencyKey;
import com.whispernetwork.simulation.application.model.SimulationRun;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

class OrmLiteSimulationRunRepositoryIntegrationTest {

    @Test
    void shouldPersistAndLoadSimulationRunRoundTrip() {
        String jdbcUrl = newTempFileDbUrl();
        migrateSchema(jdbcUrl);

        String runId = UUID.randomUUID().toString();
        SimulationRun run = new SimulationRun(runId, "network-a", 1, "actor-a", "req-a", 3);
        run.markRunning();
        run.incrementCompletedTicks();
        run.markCancelling("actor-b", "cancel-1");

        try (OrmLiteSimulationRunRepository repository = new OrmLiteSimulationRunRepository(jdbcUrl)) {
            repository.save(run);

            SimulationRun loaded = repository.findById(runId).orElseThrow();
            assertEquals(runId, loaded.getId());
            assertEquals("network-a", loaded.getNetworkId());
            assertEquals(1, loaded.getNetworkVersionNumber());
            assertEquals("actor-a", loaded.getRequestedByActorId());
            assertEquals("req-a", loaded.getClientRequestId());
            assertEquals(3, loaded.getRequestedTicks());
            assertEquals(RunStatus.CANCELLING, loaded.getStatus());
            assertEquals(1, loaded.getCompletedTicks());
            assertEquals("actor-b", loaded.getCancellationRequestedByActorId());
            assertEquals("cancel-1", loaded.getCancellationClientRequestId());
        }
    }

    @Test
    void shouldFindActiveRunAndPreserveIdempotencyMapping() {
        String jdbcUrl = newTempFileDbUrl();
        migrateSchema(jdbcUrl);

        SimulationRun active =
                new SimulationRun(UUID.randomUUID().toString(), "network-a", 1, "actor-a", "req-active", 2);
        active.markRunning();

        SimulationRun terminal =
                new SimulationRun(UUID.randomUUID().toString(), "network-a", 1, "actor-a", "req-terminal", 1);
        terminal.markRunning();
        terminal.markCompleted();

        IdempotencyKey key = new IdempotencyKey("SIMULATION_START", "actor-a", "req-active");

        try (OrmLiteSimulationRunRepository repository = new OrmLiteSimulationRunRepository(jdbcUrl)) {
            repository.save(terminal);
            repository.save(active);

            SimulationRun activeFound =
                    repository.findActiveByNetwork("network-a").orElseThrow();
            assertEquals(active.getId(), activeFound.getId());

            repository.putIdempotency(key, active.getId());
            repository.putIdempotency(key, terminal.getId());

            Optional<String> runIdByKey = repository.findByIdempotency(key);
            assertTrue(runIdByKey.isPresent());
            assertEquals(active.getId(), runIdByKey.get());
        }
    }

    private static String newTempFileDbUrl() {
        try {
            Path file = Files.createTempFile("run-repo-", ".db");
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
