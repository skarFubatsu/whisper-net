package com.whispernetwork.simulation.infrastructure.sqlite;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whispernetwork.simulation.application.port.out.InfluenceNetworkProvider;
import com.whispernetwork.simulation.core.model.AgentRole;
import com.whispernetwork.simulation.core.model.AgentState;
import com.whispernetwork.simulation.core.model.InfluenceNetwork;
import com.whispernetwork.simulation.core.model.Persona;
import com.whispernetwork.simulation.core.model.Relationship;
import com.whispernetwork.simulation.core.model.RelationshipTransmissionMode;
import com.whispernetwork.simulation.core.model.RelationshipType;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Iterator;

/**
 * JDBC-backed provider that reads persisted JSON snapshots from a SQLite database.
 */
public final class SqliteInfluenceNetworkProvider implements InfluenceNetworkProvider {
    private final String jdbcUrl;
    private final ObjectMapper mapper = new ObjectMapper();

    public SqliteInfluenceNetworkProvider(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl == null || jdbcUrl.isBlank() ? "jdbc:sqlite:network_snapshots.db" : jdbcUrl;
    }

    @Override
    public InfluenceNetwork loadNetworkSnapshot(String networkId, int networkVersionNumber) {
        try (Connection c = DriverManager.getConnection(jdbcUrl)) {
            PreparedStatement ps = c.prepareStatement(
                    "SELECT snapshot_json FROM network_snapshots WHERE network_id = ? AND version_number = ? ORDER BY created_at DESC LIMIT 1");
            ps.setString(1, networkId);
            ps.setInt(2, networkVersionNumber);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                throw new IllegalArgumentException(
                        "Network snapshot not found for id=" + networkId + ", version=" + networkVersionNumber);
            }
            String json = rs.getString(1);
            JsonNode root = mapper.readTree(json);

            InfluenceNetwork network = new InfluenceNetwork(networkId, networkVersionNumber);

            // parse agents
            JsonNode agents = root.get("agents");
            if (agents != null && agents.isArray()) {
                for (Iterator<JsonNode> it = agents.elements(); it.hasNext(); ) {
                    JsonNode a = it.next();
                    String agentId = a.get("agentId").asText();
                    String nickname =
                            a.hasNonNull("nickname") ? a.get("nickname").asText() : null;
                    String roleStr = a.hasNonNull("role") ? a.get("role").asText() : "NORMAL";
                    double bias = a.hasNonNull("bias") ? a.get("bias").asDouble() : 0.0;
                    double stubbornness =
                            a.hasNonNull("stubbornness") ? a.get("stubbornness").asDouble() : 0.0;
                    double susceptibility = a.hasNonNull("susceptibility")
                            ? a.get("susceptibility").asDouble()
                            : 0.0;
                    double suspiciousness = a.hasNonNull("suspiciousness")
                            ? a.get("suspiciousness").asDouble()
                            : 0.0;

                    Persona persona = new Persona(bias, stubbornness, susceptibility, suspiciousness);
                    AgentRole role;
                    try {
                        role = AgentRole.valueOf(roleStr.toUpperCase());
                    } catch (IllegalArgumentException ex) {
                        role = AgentRole.NORMAL;
                    }

                    AgentState agent = new AgentState(agentId, nickname, persona, role, 0.0);
                    network.addAgent(agent);
                }
            }

            // parse relationships
            JsonNode rels = root.get("relationships");
            if (rels != null && rels.isArray()) {
                for (Iterator<JsonNode> it = rels.elements(); it.hasNext(); ) {
                    JsonNode r = it.next();
                    String relId = r.hasNonNull("relationshipId")
                            ? r.get("relationshipId").asText()
                            : java.util.UUID.randomUUID().toString();
                    String source = r.get("sourceAgentId").asText();
                    String target = r.get("targetAgentId").asText();
                    double weight = r.hasNonNull("weight") ? r.get("weight").asDouble() : 0.0;
                    double trust =
                            r.hasNonNull("trustValue") ? r.get("trustValue").asDouble() : 1.0;
                    String typeStr = r.hasNonNull("relationshipType")
                            ? r.get("relationshipType").asText()
                            : "NEUTRAL";
                    String modeStr = r.hasNonNull("transmissionMode")
                            ? r.get("transmissionMode").asText()
                            : "NORMAL_FLOW";

                    RelationshipType type;
                    try {
                        type = RelationshipType.valueOf(typeStr.toUpperCase());
                    } catch (IllegalArgumentException ex) {
                        type = RelationshipType.NEUTRAL;
                    }

                    RelationshipTransmissionMode mode;
                    try {
                        mode = RelationshipTransmissionMode.valueOf(modeStr.toUpperCase());
                    } catch (IllegalArgumentException ex) {
                        mode = RelationshipTransmissionMode.NORMAL_FLOW;
                    }

                    Relationship rel = new Relationship(relId, null, source, target, weight, trust, type, mode);
                    network.addRelationship(rel);
                }
            }

            return network;
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load network snapshot", ex);
        }
    }
}
