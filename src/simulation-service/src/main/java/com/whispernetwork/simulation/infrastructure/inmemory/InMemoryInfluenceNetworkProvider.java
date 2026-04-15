package com.whispernetwork.simulation.infrastructure.inmemory;

import com.whispernetwork.simulation.application.port.out.InfluenceNetworkProvider;
import com.whispernetwork.simulation.core.model.AgentState;
import com.whispernetwork.simulation.core.model.InfluenceNetwork;
import com.whispernetwork.simulation.core.model.Relationship;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory provider for versioned influence network snapshots.
 */
public final class InMemoryInfluenceNetworkProvider implements InfluenceNetworkProvider {
    private final Map<String, InfluenceNetwork> networks;

    /**
     * Creates provider instance.
     */
    public InMemoryInfluenceNetworkProvider() {
        this.networks = new ConcurrentHashMap<>();
    }

    /**
     * Registers or replaces a network snapshot.
     *
     * @param network network snapshot
     */
    public void register(InfluenceNetwork network) {
        networks.put(key(network.getId(), network.getVersionNumber()), deepCopy(network));
    }

    @Override
    public InfluenceNetwork loadNetworkSnapshot(String networkId, int networkVersionNumber) {
        InfluenceNetwork network = networks.get(key(networkId, networkVersionNumber));
        if (network == null) {
            throw new IllegalArgumentException(
                    "Network snapshot not found for id=" + networkId + ", version=" + networkVersionNumber);
        }
        return deepCopy(network);
    }

    private static String key(String networkId, int networkVersionNumber) {
        return networkId + "#" + networkVersionNumber;
    }

    private static InfluenceNetwork deepCopy(InfluenceNetwork source) {
        InfluenceNetwork copy = new InfluenceNetwork(source.getId(), source.getVersionNumber());

        for (AgentState agent : source.getAgents()) {
            AgentState agentCopy = new AgentState(
                    agent.getId(), agent.getNickname(), agent.getPersona(), agent.getRole(), agent.getOpinionValue());
            if (agent.getRelayOriginAgentId() != null) {
                agentCopy.setRelayOriginAgentId(agent.getRelayOriginAgentId());
            }
            copy.addAgent(agentCopy);
        }

        for (Relationship relationship : source.getRelationships()) {
            copy.addRelationship(new Relationship(
                    relationship.id(),
                    relationship.reverseRelationshipId(),
                    relationship.sourceAgentId(),
                    relationship.targetAgentId(),
                    relationship.weight(),
                    relationship.trustValue(),
                    relationship.relationshipType(),
                    relationship.transmissionMode()));
        }

        return copy;
    }
}
