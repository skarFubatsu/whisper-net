package com.whispernetwork.api.application.services.catalog;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whispernetwork.api.application.dto.catalog.AgentCommand;
import com.whispernetwork.api.application.dto.catalog.AgentView;
import com.whispernetwork.api.application.dto.catalog.NetworkSnapshotRecord;
import com.whispernetwork.api.application.dto.catalog.PageResult;
import com.whispernetwork.api.application.ports.out.AgentCatalogPort;
import com.whispernetwork.api.application.ports.out.NetworkSnapshotPort;
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
class AgentCatalogApplicationServiceTest {

    @Mock
    private AgentCatalogPort agentStore;

    @Mock
    private NetworkSnapshotPort snapshotStore;

    @Mock
    private ActorContext actorContext;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AgentCatalogApplicationService service;

    @BeforeEach
    void setUp() {
        service = new AgentCatalogApplicationService(agentStore, snapshotStore, actorContext, objectMapper);
        lenient().when(actorContext.currentActorId()).thenReturn("owner-1");
    }

    @Nested
    @DisplayName("createAgent")
    class CreateAgentTests {

        @Test
        @DisplayName("generates ID when agentId is blank and passes owner metadata")
        void generatesIdAndCreatesAgent() {
            when(agentStore.create(eq("owner-1"), any(AgentView.class)))
                    .thenAnswer(inv -> inv.getArgument(1, AgentView.class));

            AgentView created = service.createAgent(new AgentCommand("", "Alpha", "NORMAL", 0.1, 0.2, 0.3, 0.4));

            assertNotNull(created.agentId());
            assertFalse(created.agentId().isBlank());
            assertEquals("Alpha", created.nickname());
            assertEquals("owner-1", created.createdBy());
            assertEquals("owner-1", created.updatedBy());
        }
    }

    @Nested
    @DisplayName("updateAgent")
    class UpdateAgentTests {

        @Test
        @DisplayName("preserves created metadata and updates updater")
        void preservesCreatedMetadata() {
            AgentView existing = new AgentView(
                    "agent-1", "Old", "NORMAL", 0.1, 0.2, 0.3, 0.4, Instant.EPOCH, Instant.EPOCH, "creator", "creator");
            when(agentStore.findByOwnerIdAndAgentId("owner-1", "agent-1")).thenReturn(Optional.of(existing));
            when(agentStore.update(eq("owner-1"), any(AgentView.class)))
                    .thenAnswer(inv -> inv.getArgument(1, AgentView.class));

            Optional<AgentView> updated =
                    service.updateAgent("agent-1", new AgentCommand("agent-1", "New", "NORMAL", 0.2, 0.2, 0.3, 0.4));

            assertTrue(updated.isPresent());
            assertEquals("creator", updated.get().createdBy());
            assertEquals("owner-1", updated.get().updatedBy());
            assertEquals(Instant.EPOCH, updated.get().createdAt());
            assertEquals("New", updated.get().nickname());
        }
    }

    @Nested
    @DisplayName("listAgents with deployed filter")
    class ListAgentsWithDeployedFilterTests {

        @Test
        @DisplayName("requires networkId when deployed filter is provided")
        void requiresNetworkId() {
            assertThrows(IllegalArgumentException.class, () -> service.listAgents(0, 20, true, null));
        }

        @Test
        @DisplayName("uses IN query when deployed=true and snapshot has agent IDs")
        void usesInQueryForDeployedTrue() {
            when(snapshotStore.findLatestSnapshot("owner-1", "net-1"))
                    .thenReturn(
                            Optional.of(
                                    snapshot(
                                            "net-1",
                                            2,
                                            """
                        {
                          "agents": [
                            {"agentId":"a-1"},
                            {"agentId":"a-2"}
                          ]
                        }
                        """)));

            PageResult<AgentView> expected = new PageResult<>(List.of(agent("a-1"), agent("a-2")), 0, 20, 2);
            when(agentStore.findPageByOwnerIdAndAgentIdIn("owner-1", List.of("a-1", "a-2"), 0, 20))
                    .thenReturn(expected);

            PageResult<AgentView> result = service.listAgents(0, 20, true, "net-1");

            assertEquals(2, result.items().size());
            verify(agentStore).findPageByOwnerIdAndAgentIdIn("owner-1", List.of("a-1", "a-2"), 0, 20);
        }

        @Test
        @DisplayName("uses NOT IN query when deployed=false and snapshot has agent IDs")
        void usesNotInQueryForDeployedFalse() {
            when(snapshotStore.findLatestSnapshot("owner-1", "net-1"))
                    .thenReturn(
                            Optional.of(
                                    snapshot(
                                            "net-1",
                                            2,
                                            """
                        {
                          "agents": [
                            {"agentId":"a-1"}
                          ]
                        }
                        """)));

            PageResult<AgentView> expected = new PageResult<>(List.of(agent("a-3")), 0, 20, 1);
            when(agentStore.findPageByOwnerIdAndAgentIdNotIn("owner-1", List.of("a-1"), 0, 20))
                    .thenReturn(expected);

            PageResult<AgentView> result = service.listAgents(0, 20, false, "net-1");

            assertEquals(1, result.items().size());
            verify(agentStore).findPageByOwnerIdAndAgentIdNotIn("owner-1", List.of("a-1"), 0, 20);
        }

        @Test
        @DisplayName("returns empty page for deployed=true when snapshot has no agents")
        void returnsEmptyWhenNoDeployedAgents() {
            when(snapshotStore.findLatestSnapshot("owner-1", "net-1"))
                    .thenReturn(Optional.of(snapshot("net-1", 2, "{\"agents\":[]}")));

            PageResult<AgentView> result = service.listAgents(0, 20, true, "net-1");

            assertEquals(0, result.total());
            assertTrue(result.items().isEmpty());
            verify(agentStore, never()).findPageByOwnerIdAndAgentIdIn(anyString(), anyList(), anyInt(), anyInt());
        }

        @Test
        @DisplayName("throws on malformed snapshot JSON")
        void throwsOnMalformedSnapshotJson() {
            when(snapshotStore.findLatestSnapshot("owner-1", "net-1"))
                    .thenReturn(Optional.of(snapshot("net-1", 2, "not-json")));

            assertThrows(IllegalArgumentException.class, () -> service.listAgents(0, 20, true, "net-1"));
        }
    }

    @Nested
    @DisplayName("listAgents pagination")
    class ListAgentsPaginationTests {

        @Test
        @DisplayName("delegates directly to owner page query when deployed filter is null")
        void delegatesWhenNoDeployedFilter() {
            PageResult<AgentView> expected = new PageResult<>(List.of(agent("a-1")), 1, 10, 11);
            when(agentStore.findPageByOwnerId("owner-1", 1, 10)).thenReturn(expected);

            PageResult<AgentView> result = service.listAgents(1, 10, null, null);

            assertEquals(1, result.page());
            verify(agentStore).findPageByOwnerId("owner-1", 1, 10);
        }

        @Test
        @DisplayName("validates pagination bounds")
        void validatesPaginationBounds() {
            assertThrows(IllegalArgumentException.class, () -> service.listAgents(-1, 10, null, null));
            assertThrows(IllegalArgumentException.class, () -> service.listAgents(0, 0, null, null));
            assertThrows(IllegalArgumentException.class, () -> service.listAgents(0, 101, null, null));
        }
    }

    @Test
    @DisplayName("createAgent passes provided ID unchanged")
    void createAgentUsesProvidedId() {
        when(agentStore.create(eq("owner-1"), any(AgentView.class)))
                .thenAnswer(inv -> inv.getArgument(1, AgentView.class));

        service.createAgent(new AgentCommand("agent-fixed", "Alpha", "NORMAL", 0.1, 0.2, 0.3, 0.4));

        ArgumentCaptor<AgentView> captor = ArgumentCaptor.forClass(AgentView.class);
        verify(agentStore).create(eq("owner-1"), captor.capture());
        assertEquals("agent-fixed", captor.getValue().agentId());
    }

    private AgentView agent(String id) {
        return new AgentView(
                id, "nick-" + id, "NORMAL", 0.1, 0.2, 0.3, 0.4, Instant.EPOCH, Instant.EPOCH, "owner-1", "owner-1");
    }

    private NetworkSnapshotRecord snapshot(String networkId, int version, String json) {
        return new NetworkSnapshotRecord(networkId, version, json, Instant.EPOCH);
    }
}
