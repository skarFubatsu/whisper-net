package com.whispernetwork.simulation.infrastructure.sqlite;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;
import com.j256.ormlite.table.DatabaseTable;
import com.whispernetwork.shared.dto.SimulationHistoryEntry;
import com.whispernetwork.shared.dto.TimelineQuery;
import com.whispernetwork.simulation.application.model.SimulationEvent;
import com.whispernetwork.simulation.application.port.out.SimulationHistoryRepository;
import com.whispernetwork.simulation.core.engine.AgentOpinionUpdate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * ORMLite repository for simulation event and opinion-update history.
 */
public final class OrmLiteSimulationHistoryRepository implements SimulationHistoryRepository, AutoCloseable {
    private final JdbcConnectionSource connectionSource;
    private final Dao<SimulationEventRow, Long> eventDao;
    private final Dao<OpinionUpdateRow, Long> opinionUpdateDao;

    /**
     * Creates repository using the provided JDBC URL.
     */
    public OrmLiteSimulationHistoryRepository(String jdbcUrl) {
        try {
            this.connectionSource = new JdbcConnectionSource(jdbcUrl);
            this.eventDao = DaoManager.createDao(connectionSource, SimulationEventRow.class);
            this.opinionUpdateDao = DaoManager.createDao(connectionSource, OpinionUpdateRow.class);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to initialize ORMLite history repository", ex);
        }
    }

    @Override
    public synchronized void record(SimulationEvent event) {
        try {
            SimulationEventRow eventRow = SimulationEventRow.fromEvent(event);
            eventDao.create(eventRow);

            if (event instanceof SimulationEvent.SimulationTickCompleted tickCompleted) {
                for (AgentOpinionUpdate update : tickCompleted.updates()) {
                    OpinionUpdateRow opinionRow = OpinionUpdateRow.fromUpdate(eventRow.id, tickCompleted, update);
                    opinionUpdateDao.create(opinionRow);
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to persist simulation history", ex);
        }
    }

    @Override
    public synchronized List<SimulationHistoryEntry> list(TimelineQuery query) {
        try {
            QueryBuilder<SimulationEventRow, Long> builder = eventDao.queryBuilder();
            Where<SimulationEventRow, Long> where = builder.where();
            List<String> clauses = new ArrayList<>();

            if (query.simulationRunId() != null && !query.simulationRunId().isBlank()) {
                where.eq("simulation_run_id", query.simulationRunId());
                clauses.add("run");
            }

            if (query.networkId() != null && !query.networkId().isBlank()) {
                if (!clauses.isEmpty()) {
                    where.and();
                }
                where.eq("network_id", query.networkId());
                clauses.add("network");
            }

            if (!query.eventTypes().isEmpty()) {
                if (!clauses.isEmpty()) {
                    where.and();
                }
                where.in("event_type", query.eventTypes());
                clauses.add("types");
            }

            if (query.window().fromEpochMillis() != null) {
                if (!clauses.isEmpty()) {
                    where.and();
                }
                where.ge("occurred_at_epoch_millis", query.window().fromEpochMillis());
                clauses.add("from");
            }

            if (query.window().toEpochMillis() != null) {
                if (!clauses.isEmpty()) {
                    where.and();
                }
                where.le("occurred_at_epoch_millis", query.window().toEpochMillis());
                clauses.add("to");
            }

            builder.orderBy("occurred_at_epoch_millis", false);
            builder.limit(query.window().limit().longValue());
            builder.offset(query.window().offset());

            List<SimulationEventRow> rows = eventDao.query(builder.prepare());
            List<SimulationHistoryEntry> entries = new ArrayList<>(rows.size());
            for (SimulationEventRow row : rows) {
                entries.add(row.toEntry());
            }
            return entries;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to query simulation history", ex);
        }
    }

    @Override
    public void close() {
        connectionSource.closeQuietly();
    }

    @DatabaseTable(tableName = "simulation_events")
    private static final class SimulationEventRow {
        @DatabaseField(generatedId = true)
        private Long id;

        @DatabaseField(columnName = "simulation_run_id", canBeNull = false)
        private String simulationRunId;

        @DatabaseField(columnName = "network_id", canBeNull = false)
        private String networkId;

        @DatabaseField(columnName = "owner_id")
        private String ownerId;

        @DatabaseField(columnName = "request_id")
        private String requestId;

        @DatabaseField(columnName = "event_type", canBeNull = false)
        private String eventType;

        @DatabaseField(columnName = "tick_number")
        private Integer tickNumber;

        @DatabaseField(columnName = "updated_agents")
        private Integer updatedAgents;

        @DatabaseField(columnName = "actor_id")
        private String actorId;

        @DatabaseField(columnName = "client_request_id")
        private String clientRequestId;

        @DatabaseField(columnName = "reason")
        private String reason;

        @DatabaseField(columnName = "completed_ticks")
        private Integer completedTicks;

        @DatabaseField(columnName = "occurred_at_epoch_millis", canBeNull = false)
        private long occurredAtEpochMillis;

        @DatabaseField(columnName = "created_at_epoch_millis", canBeNull = false)
        private long createdAtEpochMillis;

        private static SimulationEventRow fromEvent(SimulationEvent event) {
            SimulationEventRow row = new SimulationEventRow();
            row.simulationRunId = event.simulationRunId();
            row.networkId = event.networkId();
            row.ownerId = event.ownerId();
            row.requestId = event.requestId();
            row.eventType = eventType(event);
            row.occurredAtEpochMillis = event.occurredAt().toEpochMilli();
            row.createdAtEpochMillis = Instant.now().toEpochMilli();

            if (event instanceof SimulationEvent.SimulationStarted started) {
                row.actorId = started.initiatedByActorId();
                row.clientRequestId = started.clientRequestId();
            } else if (event instanceof SimulationEvent.SimulationCancelled cancelled) {
                row.actorId = cancelled.cancelledByActorId();
                row.clientRequestId = cancelled.clientRequestId();
            } else if (event instanceof SimulationEvent.SimulationTickCompleted tickCompleted) {
                row.tickNumber = tickCompleted.tickNumber();
                row.updatedAgents = tickCompleted.updatedAgents();
            } else if (event instanceof SimulationEvent.SimulationCompleted completed) {
                row.completedTicks = completed.completedTicks();
            } else if (event instanceof SimulationEvent.SimulationFailed failed) {
                row.reason = failed.reason();
            }

            return row;
        }

        private SimulationHistoryEntry toEntry() {
            return new SimulationHistoryEntry(
                    id == null ? -1L : id,
                    simulationRunId,
                    networkId,
                    eventType,
                    tickNumber,
                    updatedAgents,
                    actorId,
                    clientRequestId,
                    reason,
                    completedTicks,
                    occurredAtEpochMillis,
                    createdAtEpochMillis);
        }

        private static String eventType(SimulationEvent event) {
            if (event instanceof SimulationEvent.SimulationStarted) {
                return "SIMULATION_STARTED";
            }
            if (event instanceof SimulationEvent.SimulationCancelled) {
                return "SIMULATION_CANCELLED";
            }
            if (event instanceof SimulationEvent.SimulationTickCompleted) {
                return "SIMULATION_TICK_COMPLETED";
            }
            if (event instanceof SimulationEvent.SimulationCompleted) {
                return "SIMULATION_COMPLETED";
            }
            if (event instanceof SimulationEvent.SimulationFailed) {
                return "SIMULATION_FAILED";
            }
            throw new IllegalStateException(
                    "Unsupported event type: " + event.getClass().getName());
        }
    }

    @DatabaseTable(tableName = "simulation_opinion_updates")
    private static final class OpinionUpdateRow {
        @DatabaseField(generatedId = true)
        private Long id;

        @DatabaseField(columnName = "event_id", canBeNull = false)
        private long eventId;

        @DatabaseField(columnName = "simulation_run_id", canBeNull = false)
        private String simulationRunId;

        @DatabaseField(columnName = "network_id", canBeNull = false)
        private String networkId;

        @DatabaseField(columnName = "tick_number", canBeNull = false)
        private int tickNumber;

        @DatabaseField(columnName = "agent_id", canBeNull = false)
        private String agentId;

        @DatabaseField(columnName = "previous_opinion_value", canBeNull = false)
        private double previousOpinionValue;

        @DatabaseField(columnName = "new_opinion_value", canBeNull = false)
        private double newOpinionValue;

        @DatabaseField(columnName = "incoming_influence_magnitude", canBeNull = false)
        private double incomingInfluenceMagnitude;

        @DatabaseField(columnName = "relayed_as_is", canBeNull = false)
        private boolean relayedAsIs;

        @DatabaseField(columnName = "relay_origin_agent_id")
        private String relayOriginAgentId;

        @DatabaseField(columnName = "ignored_trust_and_weight", canBeNull = false)
        private boolean ignoredTrustAndWeight;

        @DatabaseField(columnName = "contributing_source_agent_ids", canBeNull = false, dataType = DataType.LONG_STRING)
        private String contributingSourceAgentIds;

        @DatabaseField(columnName = "occurred_at_epoch_millis", canBeNull = false)
        private long occurredAtEpochMillis;

        private static OpinionUpdateRow fromUpdate(
                long eventId, SimulationEvent.SimulationTickCompleted tickCompleted, AgentOpinionUpdate update) {
            OpinionUpdateRow row = new OpinionUpdateRow();
            row.eventId = eventId;
            row.simulationRunId = tickCompleted.simulationRunId();
            row.networkId = tickCompleted.networkId();
            row.tickNumber = tickCompleted.tickNumber();
            row.agentId = update.agentId();
            row.previousOpinionValue = update.previousOpinionValue();
            row.newOpinionValue = update.newOpinionValue();
            row.incomingInfluenceMagnitude = update.incomingInfluenceMagnitude();
            row.relayedAsIs = update.relayedAsIs();
            row.relayOriginAgentId = update.relayOriginAgentId();
            row.ignoredTrustAndWeight = update.ignoredTrustAndWeight();
            row.contributingSourceAgentIds = joinSourceIds(update.contributingSourceAgentIds());
            row.occurredAtEpochMillis = tickCompleted.occurredAt().toEpochMilli();
            return row;
        }

        private static String joinSourceIds(List<String> ids) {
            if (ids == null || ids.isEmpty()) {
                return "";
            }
            StringJoiner joiner = new StringJoiner(",");
            for (String id : ids) {
                joiner.add(id);
            }
            return joiner.toString();
        }
    }
}
