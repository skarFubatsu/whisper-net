package com.whispernetwork.simulation.infrastructure.sqlite;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.table.DatabaseTable;
import com.whispernetwork.shared.dto.RunStatus;
import com.whispernetwork.simulation.application.model.IdempotencyKey;
import com.whispernetwork.simulation.application.model.SimulationRun;
import com.whispernetwork.simulation.application.port.out.SimulationRunRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * ORMLite repository for simulation run state and idempotency records.
 */
public final class OrmLiteSimulationRunRepository implements SimulationRunRepository, AutoCloseable {
  private static final List<String> ACTIVE_STATUSES = List.of("REQUESTED", "RUNNING", "CANCELLING");

  private final JdbcConnectionSource connectionSource;
  private final Dao<SimulationRunRow, String> runDao;
  private final Dao<IdempotencyRow, Long> idempotencyDao;

  /**
   * Creates repository using the provided JDBC URL.
   */
  public OrmLiteSimulationRunRepository(String jdbcUrl) {
    try {
      this.connectionSource = new JdbcConnectionSource(jdbcUrl);
      this.runDao = DaoManager.createDao(connectionSource, SimulationRunRow.class);
      this.idempotencyDao = DaoManager.createDao(connectionSource, IdempotencyRow.class);
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to initialize ORMLite run repository", ex);
    }
  }

  @Override
  public synchronized void save(SimulationRun run) {
    try {
      runDao.createOrUpdate(SimulationRunRow.fromDomain(run));
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to persist simulation run", ex);
    }
  }

  @Override
  public synchronized Optional<SimulationRun> findById(String runId) {
    try {
      SimulationRunRow row = runDao.queryForId(runId);
      return row == null ? Optional.empty() : Optional.of(row.toDomain());
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to query simulation run by id", ex);
    }
  }

  @Override
  public synchronized Optional<SimulationRun> findActiveByNetwork(String networkId) {
    try {
      QueryBuilder<SimulationRunRow, String> query = runDao.queryBuilder();
      query.where().eq("network_id", networkId).and().in("status", ACTIVE_STATUSES);
      query.orderBy("updated_at_epoch_millis", false);
      query.limit(1L);
      List<SimulationRunRow> rows = runDao.query(query.prepare());
      if (rows.isEmpty()) {
        return Optional.empty();
      }
      return Optional.of(rows.get(0).toDomain());
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to query active simulation run", ex);
    }
  }

  @Override
  public synchronized void putIdempotency(IdempotencyKey key, String runId) {
    try {
      Optional<String> existing = findByIdempotency(key);
      if (existing.isPresent()) {
        return;
      }

      IdempotencyRow row = new IdempotencyRow();
      row.commandType = key.commandType();
      row.actorId = key.actorId();
      row.clientRequestId = key.clientRequestId();
      row.runId = runId;
      row.createdAtEpochMillis = Instant.now().toEpochMilli();
      idempotencyDao.create(row);
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to persist idempotency key", ex);
    }
  }

  @Override
  public synchronized Optional<String> findByIdempotency(IdempotencyKey key) {
    try {
      QueryBuilder<IdempotencyRow, Long> query = idempotencyDao.queryBuilder();
      query.where()
          .eq("command_type", key.commandType())
          .and()
          .eq("actor_id", key.actorId())
          .and()
          .eq("client_request_id", key.clientRequestId());
      query.limit(1L);

      IdempotencyRow row = idempotencyDao.queryForFirst(query.prepare());
      return row == null ? Optional.empty() : Optional.of(row.runId);
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to query idempotency key", ex);
    }
  }

  @Override
  public void close() {
    connectionSource.closeQuietly();
  }

  @DatabaseTable(tableName = "simulation_runs")
  private static final class SimulationRunRow {
    @DatabaseField(columnName = "run_id", id = true)
    private String runId;

    @DatabaseField(columnName = "network_id", canBeNull = false)
    private String networkId;

    @DatabaseField(columnName = "network_version_number", canBeNull = false)
    private int networkVersionNumber;

    @DatabaseField(columnName = "requested_by_actor_id", canBeNull = false)
    private String requestedByActorId;

    @DatabaseField(columnName = "client_request_id", canBeNull = false)
    private String clientRequestId;

    @DatabaseField(columnName = "requested_ticks", canBeNull = false)
    private int requestedTicks;

    @DatabaseField(columnName = "status", canBeNull = false)
    private String status;

    @DatabaseField(columnName = "completed_ticks", canBeNull = false)
    private int completedTicks;

    @DatabaseField(columnName = "failure_message")
    private String failureMessage;

    @DatabaseField(columnName = "cancellation_requested_by_actor_id")
    private String cancellationRequestedByActorId;

    @DatabaseField(columnName = "cancellation_client_request_id")
    private String cancellationClientRequestId;

    @DatabaseField(columnName = "created_at_epoch_millis", canBeNull = false)
    private long createdAtEpochMillis;

    @DatabaseField(columnName = "updated_at_epoch_millis", canBeNull = false)
    private long updatedAtEpochMillis;

    private static SimulationRunRow fromDomain(SimulationRun run) {
      SimulationRunRow row = new SimulationRunRow();
      row.runId = run.getId();
      row.networkId = run.getNetworkId();
      row.networkVersionNumber = run.getNetworkVersionNumber();
      row.requestedByActorId = run.getRequestedByActorId();
      row.clientRequestId = run.getClientRequestId();
      row.requestedTicks = run.getRequestedTicks();
      row.status = run.getStatus().name();
      row.completedTicks = run.getCompletedTicks();
      row.failureMessage = run.getFailureMessage();
      row.cancellationRequestedByActorId = run.getCancellationRequestedByActorId();
      row.cancellationClientRequestId = run.getCancellationClientRequestId();
      row.createdAtEpochMillis = run.getCreatedAt().toEpochMilli();
      row.updatedAtEpochMillis = run.getUpdatedAt().toEpochMilli();
      return row;
    }

    private SimulationRun toDomain() {
      return SimulationRun.rehydrate(
          runId,
          networkId,
          networkVersionNumber,
          requestedByActorId,
          clientRequestId,
          requestedTicks,
          RunStatus.valueOf(status),
          completedTicks,
          failureMessage,
          cancellationRequestedByActorId,
          cancellationClientRequestId,
          Instant.ofEpochMilli(createdAtEpochMillis),
          Instant.ofEpochMilli(updatedAtEpochMillis));
    }
  }

  @DatabaseTable(tableName = "simulation_idempotency_keys")
  private static final class IdempotencyRow {
    @DatabaseField(generatedId = true)
    private Long id;

    @DatabaseField(columnName = "command_type", canBeNull = false, uniqueCombo = true)
    private String commandType;

    @DatabaseField(columnName = "actor_id", canBeNull = false, uniqueCombo = true)
    private String actorId;

    @DatabaseField(columnName = "client_request_id", canBeNull = false, uniqueCombo = true)
    private String clientRequestId;

    @DatabaseField(columnName = "run_id", canBeNull = false)
    private String runId;

    @DatabaseField(columnName = "created_at_epoch_millis", canBeNull = false)
    private long createdAtEpochMillis;
  }
}
