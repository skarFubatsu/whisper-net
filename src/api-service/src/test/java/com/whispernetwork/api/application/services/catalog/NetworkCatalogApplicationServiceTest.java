package com.whispernetwork.api.application.services.catalog;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whispernetwork.api.application.dto.catalog.AgentSnapshot;
import com.whispernetwork.api.application.dto.catalog.AgentView;
import com.whispernetwork.api.application.dto.catalog.NetworkAgentCommand;
import com.whispernetwork.api.application.dto.catalog.NetworkCommand;
import com.whispernetwork.api.application.dto.catalog.NetworkSnapshotRecord;
import com.whispernetwork.api.application.dto.catalog.NetworkVersionCommand;
import com.whispernetwork.api.application.dto.catalog.NetworkVersionView;
import com.whispernetwork.api.application.dto.catalog.NetworkView;
import com.whispernetwork.api.application.dto.catalog.RelationshipCommand;
import com.whispernetwork.api.application.error.BadRequestException;
import com.whispernetwork.api.application.error.ConflictException;
import com.whispernetwork.api.application.ports.out.AgentCatalogPort;
import com.whispernetwork.api.application.ports.out.NetworkCatalogPort;
import com.whispernetwork.api.application.ports.out.NetworkSnapshotPort;
import com.whispernetwork.api.application.ports.out.NetworkSsePublisherPort;
import com.whispernetwork.api.application.security.ActorContext;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NetworkCatalogApplicationServiceTest {

    @Mock
    private NetworkCatalogPort networkStore;

    @Mock
    private NetworkSnapshotPort snapshotStore;

    @Mock
    private AgentCatalogPort agentStore;

    @Mock
    private ActorContext actorContext;

    @Mock
    private NetworkSsePublisherPort ssePublisher;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private NetworkCatalogApplicationService service;

    @BeforeEach
    void setUp() {
        service = new NetworkCatalogApplicationService(
                networkStore, snapshotStore, agentStore, actorContext, objectMapper, ssePublisher);
        when(actorContext.currentActorId()).thenReturn("owner-1");
    }

    @Nested
    @DisplayName("createNetwork")
    class CreateNetworkTests {

        @Test
        @DisplayName("creates network and persists draft snapshot at version 0")
        void createsNetworkAndSavesDraft() {
            when(networkStore.create(eq("owner-1"), any(NetworkView.class)))
                    .thenAnswer(inv -> inv.getArgument(1, NetworkView.class));

            NetworkView created = service.createNetwork(new NetworkCommand("Network A", null));

            assertNotNull(created.networkId());
            assertEquals("Network A", created.name());

            ArgumentCaptor<NetworkSnapshotRecord> recordCaptor = ArgumentCaptor.forClass(NetworkSnapshotRecord.class);
            verify(snapshotStore).saveSnapshot(eq("owner-1"), recordCaptor.capture());

            NetworkSnapshotRecord saved = recordCaptor.getValue();
            assertEquals(created.networkId(), saved.networkId());
            assertEquals(0, saved.versionNumber());

            JsonNode root;
            try {
                root = objectMapper.readTree(saved.snapshotJson());
            } catch (Exception ex) {
                fail("Snapshot JSON must be valid", ex);
                return;
            }
            assertTrue(root.path("agents").isArray());
            assertTrue(root.path("relationships").isArray());
            assertEquals(0, root.path("agents").size());
            assertEquals(0, root.path("relationships").size());
        }
    }

    @Nested
    @DisplayName("deployAgent")
    class DeployAgentTests {

        @Test
        @DisplayName("deploys agent into draft snapshot and publishes event")
        void deploysAgentIntoDraft() throws Exception {
            when(networkStore.findByOwnerIdAndNetworkId("owner-1", "net-1"))
                    .thenReturn(Optional.of(new NetworkView("net-1", "Net", Instant.EPOCH, Instant.EPOCH)));

            AgentView agent = agentView("agent-1", "Alpha");
            when(agentStore.findByOwnerIdAndAgentId("owner-1", "agent-1")).thenReturn(Optional.of(agent));
            when(snapshotStore.findSnapshot("owner-1", "net-1", 0)).thenReturn(Optional.empty());

            service.deployAgent("net-1", new NetworkAgentCommand("agent-1"));

            ArgumentCaptor<NetworkSnapshotRecord> recordCaptor = ArgumentCaptor.forClass(NetworkSnapshotRecord.class);
            verify(snapshotStore).saveSnapshot(eq("owner-1"), recordCaptor.capture());
            verify(ssePublisher).publishEvent("owner-1", "net-1", "agent.deployed", agent);

            JsonNode root = objectMapper.readTree(recordCaptor.getValue().snapshotJson());
            assertEquals(1, root.path("agents").size());
            assertEquals("agent-1", root.path("agents").get(0).path("agentId").asText());
            assertEquals(0, recordCaptor.getValue().versionNumber());
        }

        @Test
        @DisplayName("rejects duplicate deployment when agent already in draft")
        void rejectsDuplicateDeployment() {
            when(networkStore.findByOwnerIdAndNetworkId("owner-1", "net-1"))
                    .thenReturn(Optional.of(new NetworkView("net-1", "Net", Instant.EPOCH, Instant.EPOCH)));
            when(agentStore.findByOwnerIdAndAgentId("owner-1", "agent-1"))
                    .thenReturn(Optional.of(agentView("agent-1", "Alpha")));
            when(snapshotStore.findSnapshot("owner-1", "net-1", 0))
                    .thenReturn(
                            Optional.of(
                                    snapshotRecord(
                                            "net-1",
                                            0,
                                            """
                            {
                              "agents": [
                                {
                                  "agentId": "agent-1",
                                  "nickname": "Alpha",
                                  "role": "NORMAL",
                                  "bias": 0.1,
                                  "stubbornness": 0.2,
                                  "susceptibility": 0.3,
                                  "suspiciousness": 0.4
                                }
                              ],
                              "relationships": []
                            }
                            """)));

            assertThrows(
                    ConflictException.class, () -> service.deployAgent("net-1", new NetworkAgentCommand("agent-1")));

            verify(snapshotStore, never()).saveSnapshot(anyString(), any(NetworkSnapshotRecord.class));
            verify(ssePublisher, never()).publishEvent(anyString(), anyString(), anyString(), any());
        }
    }

    @Nested
    @DisplayName("createRelationship")
    class CreateRelationshipTests {

        @Test
        @DisplayName("creates mirrored relationship pair and saves both edges")
        void createsMirroredPair() throws Exception {
            when(networkStore.findByOwnerIdAndNetworkId("owner-1", "net-1"))
                    .thenReturn(Optional.of(new NetworkView("net-1", "Net", Instant.EPOCH, Instant.EPOCH)));

            AgentView source = agentView("a-1", "A1");
            AgentView target = agentView("a-2", "A2");
            when(agentStore.findByOwnerIdAndAgentId("owner-1", "a-1")).thenReturn(Optional.of(source));
            when(agentStore.findByOwnerIdAndAgentId("owner-1", "a-2")).thenReturn(Optional.of(target));

            when(snapshotStore.findSnapshot("owner-1", "net-1", 0))
                    .thenReturn(
                            Optional.of(
                                    snapshotRecord(
                                            "net-1",
                                            0,
                                            """
                            {
                              "agents": [
                                {"agentId":"a-1","nickname":"A1","role":"NORMAL","bias":0.1,"stubbornness":0.2,"susceptibility":0.3,"suspiciousness":0.4},
                                {"agentId":"a-2","nickname":"A2","role":"NORMAL","bias":0.1,"stubbornness":0.2,"susceptibility":0.3,"suspiciousness":0.4}
                              ],
                              "relationships": []
                            }
                            """)));

            service.createRelationship(
                    "net-1",
                    new RelationshipCommand(null, null, null, "a-1", "a-2", 0.8, 0.7, "FRIEND", "DIRECT"),
                    true);

            ArgumentCaptor<NetworkSnapshotRecord> recordCaptor = ArgumentCaptor.forClass(NetworkSnapshotRecord.class);
            verify(snapshotStore).saveSnapshot(eq("owner-1"), recordCaptor.capture());

            JsonNode root = objectMapper.readTree(recordCaptor.getValue().snapshotJson());
            JsonNode rels = root.path("relationships");
            assertEquals(2, rels.size());
            assertEquals("a-1", rels.get(0).path("sourceAgentId").asText());
            assertEquals("a-2", rels.get(0).path("targetAgentId").asText());
            assertEquals("a-2", rels.get(1).path("sourceAgentId").asText());
            assertEquals("a-1", rels.get(1).path("targetAgentId").asText());
        }

        @Test
        @DisplayName("rejects self relationship")
        void rejectsSelfRelationship() {
            when(networkStore.findByOwnerIdAndNetworkId("owner-1", "net-1"))
                    .thenReturn(Optional.of(new NetworkView("net-1", "Net", Instant.EPOCH, Instant.EPOCH)));

            assertThrows(
                    BadRequestException.class,
                    () -> service.createRelationship(
                            "net-1",
                            new RelationshipCommand(null, null, null, "a-1", "a-1", 1.0, 1.0, "FRIEND", "DIRECT"),
                            true));
        }
    }

    @Nested
    @DisplayName("createVersion")
    class CreateVersionTests {

        @Test
        @DisplayName("rejects base version mismatch")
        void rejectsBaseVersionMismatch() {
            when(networkStore.findByOwnerIdAndNetworkId("owner-1", "net-1"))
                    .thenReturn(Optional.of(new NetworkView("net-1", "Net", Instant.EPOCH, Instant.EPOCH)));
            when(networkStore.countVersions("owner-1", "net-1")).thenReturn(2);

            assertThrows(
                    ConflictException.class,
                    () -> service.createVersion("net-1", new NetworkVersionCommand("v3", 1, List.of(), List.of())));
        }

        @Test
        @DisplayName("creates next version and saves normalized snapshot")
        void createsNextVersionAndSavesSnapshot() throws Exception {
            when(networkStore.findByOwnerIdAndNetworkId("owner-1", "net-1"))
                    .thenReturn(Optional.of(new NetworkView("net-1", "Net", Instant.EPOCH, Instant.EPOCH)));
            when(networkStore.countVersions("owner-1", "net-1")).thenReturn(0);

            AgentSnapshot a1 = new AgentSnapshot("a-1", "A1", "NORMAL", 0.1, 0.2, 0.3, 0.4);
            AgentSnapshot a2 = new AgentSnapshot("a-2", "A2", "NORMAL", 0.1, 0.2, 0.3, 0.4);
            when(agentStore.findByOwnerIdAndAgentId("owner-1", "a-1")).thenReturn(Optional.of(agentView("a-1", "A1")));
            when(agentStore.findByOwnerIdAndAgentId("owner-1", "a-2")).thenReturn(Optional.of(agentView("a-2", "A2")));

            when(networkStore.createVersion(eq("owner-1"), any(NetworkVersionView.class)))
                    .thenAnswer(inv -> inv.getArgument(1, NetworkVersionView.class));

            NetworkVersionView saved = service.createVersion(
                    "net-1", new NetworkVersionCommand("Initial release", 0, List.of(a1, a2), List.of()));

            assertEquals(1, saved.versionNumber());
            assertEquals("net-1", saved.networkId());

            ArgumentCaptor<NetworkSnapshotRecord> recordCaptor = ArgumentCaptor.forClass(NetworkSnapshotRecord.class);
            verify(snapshotStore, atLeast(2)).saveSnapshot(eq("owner-1"), recordCaptor.capture());

            NetworkSnapshotRecord versionRecord = recordCaptor.getAllValues().stream()
                    .filter(r -> r.versionNumber() == 1)
                    .findFirst()
                    .orElseThrow();

            JsonNode root = objectMapper.readTree(versionRecord.snapshotJson());
            assertEquals(2, root.path("agents").size());
            assertTrue(root.path("relationships").isArray());
            verify(ssePublisher).publishEvent(eq("owner-1"), eq("net-1"), eq("version.deployed"), any());
        }
    }

    private AgentView agentView(String id, String nickname) {
        return new AgentView(
                id, nickname, "NORMAL", 0.1, 0.2, 0.3, 0.4, Instant.EPOCH, Instant.EPOCH, "owner-1", "owner-1");
    }

    private NetworkSnapshotRecord snapshotRecord(String networkId, int versionNumber, String json) {
        return new NetworkSnapshotRecord(networkId, versionNumber, json, Instant.EPOCH);
    }
}
