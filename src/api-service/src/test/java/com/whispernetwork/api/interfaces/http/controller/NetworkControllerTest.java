package com.whispernetwork.api.interfaces.http.controller;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.whispernetwork.api.application.dto.catalog.*;
import com.whispernetwork.api.application.security.ActorContext;
import com.whispernetwork.api.application.services.catalog.NetworkCatalogApplicationService;
import com.whispernetwork.api.infrastructure.stream.NetworkSseBroadcaster;
import com.whispernetwork.api.interfaces.http.error.ApiExceptionHandler;
import com.whispernetwork.api.interfaces.http.mapper.AgentHttpMapper;
import com.whispernetwork.api.interfaces.http.mapper.NetworkHttpMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@WebMvcTest(NetworkController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ApiExceptionHandler.class, NetworkHttpMapper.class, AgentHttpMapper.class})
@SuppressWarnings("nullness")
class NetworkControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NetworkCatalogApplicationService service;

    @MockBean
    private NetworkSseBroadcaster broadcaster;

    @MockBean
    private ActorContext actorContext;

    @Nested
    @DisplayName("GET /api/networks")
    class ListNetworksTests {
        @Test
        @DisplayName("returns list of networks")
        void listNetworks() throws Exception {
            List<NetworkView> networks = List.of(
                    new NetworkView("net-1", "Network 1", Instant.EPOCH, Instant.EPOCH),
                    new NetworkView("net-2", "Network 2", Instant.EPOCH, Instant.EPOCH));
            when(service.listNetworks()).thenReturn(networks);

            mockMvc.perform(get("/api/networks").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].networkId").value("net-1"))
                    .andExpect(jsonPath("$[1].networkId").value("net-2"));
        }
    }

    @Nested
    @DisplayName("POST /api/networks")
    class CreateNetworkTests {
        @Test
        @DisplayName("creates network and returns 201 with Location header")
        void createsNetwork() throws Exception {
            NetworkView created = new NetworkView("net-1", "New Network", Instant.now(), Instant.now());
            when(service.createNetwork(any())).thenReturn(created);

            mockMvc.perform(post("/api/networks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"New Network\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(header().string("Location", containsString("/api/networks/net-1")))
                    .andExpect(jsonPath("$.networkId").value("net-1"))
                    .andExpect(jsonPath("$.name").value("New Network"));
        }

        @Test
        @DisplayName("validates network name is required")
        void validatesNetworkNameRequired() throws Exception {
            mockMvc.perform(post("/api/networks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }
    }

    @Nested
    @DisplayName("GET /api/networks/{networkId}/agents")
    class ListNetworkAgentsTests {
        @Test
        @DisplayName("returns paginated list of network agents")
        void listNetworkAgents() throws Exception {
            PageResult<AgentView> pageResult = new PageResult<>(
                    List.of(new AgentView(
                            "agent-1",
                            "alpha",
                            "NORMAL",
                            0.1,
                            0.2,
                            0.3,
                            0.4,
                            Instant.EPOCH,
                            Instant.EPOCH,
                            "owner",
                            "owner")),
                    0,
                    50,
                    1);
            when(service.listNetworkAgents("net-1", 0, 50)).thenReturn(pageResult);

            mockMvc.perform(get("/api/networks/net-1/agents").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items", hasSize(1)))
                    .andExpect(jsonPath("$.items[0].agentId").value("agent-1"))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(50))
                    .andExpect(jsonPath("$.total").value(1));
        }

        @Test
        @DisplayName("enforces max page size of 100")
        void enforcesMaxPageSize() throws Exception {
            when(service.listNetworkAgents("net-1", 0, 101))
                    .thenThrow(new IllegalArgumentException("size must be between 1 and 100"));

            mockMvc.perform(get("/api/networks/net-1/agents?size=101").accept(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("size must be between 1 and 100"));
        }

        @Test
        @DisplayName("validates page parameter is non-negative")
        void validatesPageParameter() throws Exception {
            when(service.listNetworkAgents("net-1", -1, 50))
                    .thenThrow(new IllegalArgumentException("page must be >= 0"));

            mockMvc.perform(get("/api/networks/net-1/agents?page=-1").accept(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("page must be >= 0"));
        }
    }

    @Nested
    @DisplayName("POST /api/networks/{networkId}/agents")
    class DeployAgentTests {
        @Test
        @DisplayName("deploys agent and returns 201 with Location header")
        void deploysAgent() throws Exception {
            AgentView agent = new AgentView(
                    "agent-1", "alpha", "NORMAL", 0.1, 0.2, 0.3, 0.4, Instant.EPOCH, Instant.EPOCH, "owner", "owner");
            when(service.deployAgent(eq("net-1"), any())).thenReturn(agent);

            mockMvc.perform(post("/api/networks/net-1/agents")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"agentId\":\"agent-1\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(header().string("Location", containsString("/api/networks/net-1/agents/agent-1")))
                    .andExpect(jsonPath("$.agentId").value("agent-1"));
        }

        @Test
        @DisplayName("returns 400 when agent ID is missing")
        void validatesAgentIdRequired() throws Exception {
            mockMvc.perform(post("/api/networks/net-1/agents")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 404 when agent not found")
        void returnsNotFoundWhenAgentNotFound() throws Exception {
            when(service.deployAgent(eq("net-1"), any()))
                    .thenThrow(new com.whispernetwork.api.application.error.NotFoundException("Agent not found"));

            mockMvc.perform(post("/api/networks/net-1/agents")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"agentId\":\"unknown\"}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @DisplayName("returns 409 when agent already deployed")
        void returnsConflictWhenAlreadyDeployed() throws Exception {
            when(service.deployAgent(eq("net-1"), any()))
                    .thenThrow(
                            new com.whispernetwork.api.application.error.ConflictException("Agent already deployed"));

            mockMvc.perform(post("/api/networks/net-1/agents")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"agentId\":\"agent-1\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409));
        }
    }

    @Nested
    @DisplayName("DELETE /api/networks/{networkId}/agents/{agentId}")
    class UndeployAgentTests {
        @Test
        @DisplayName("undeployeds agent and returns 204")
        void undeploysAgent() throws Exception {
            mockMvc.perform(delete("/api/networks/net-1/agents/agent-1")).andExpect(status().isNoContent());

            verify(service).undeployAgent("net-1", "agent-1");
        }

        @Test
        @DisplayName("returns 404 when agent not deployed")
        void returnsNotFoundWhenNotDeployed() throws Exception {
            doThrow(new com.whispernetwork.api.application.error.NotFoundException("Agent not deployed"))
                    .when(service)
                    .undeployAgent("net-1", "unknown");

            mockMvc.perform(delete("/api/networks/net-1/agents/unknown")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/networks/{networkId}/relationships")
    class ListRelationshipsTests {
        @Test
        @DisplayName("returns paginated list of relationships")
        void listRelationships() throws Exception {
            PageResult<RelationshipView> pageResult = new PageResult<>(
                    List.of(new RelationshipView(
                            "rel-1", null, null, "agent-1", "agent-2", 1.0, 0.5, "FRIEND", "DIRECT")),
                    0,
                    50,
                    1);
            when(service.listNetworkRelationships(eq("net-1"), eq(0), eq(50), isNull(), isNull(), isNull()))
                    .thenReturn(pageResult);

            mockMvc.perform(get("/api/networks/net-1/relationships").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items", hasSize(1)))
                    .andExpect(jsonPath("$.items[0].relationshipId").value("rel-1"));
        }

        @Test
        @DisplayName("filters by source agent ID")
        void filtersbySourceAgentId() throws Exception {
            PageResult<RelationshipView> pageResult = new PageResult<>(List.of(), 0, 50, 0);
            when(service.listNetworkRelationships(eq("net-1"), eq(0), eq(50), eq("agent-1"), isNull(), isNull()))
                    .thenReturn(pageResult);

            mockMvc.perform(get("/api/networks/net-1/relationships?sourceAgentId=agent-1")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            verify(service).listNetworkRelationships("net-1", 0, 50, "agent-1", null, null);
        }

        @Test
        @DisplayName("filters by mirror flag")
        void filtersByMirrorFlag() throws Exception {
            PageResult<RelationshipView> pageResult = new PageResult<>(List.of(), 0, 50, 0);
            when(service.listNetworkRelationships(eq("net-1"), eq(0), eq(50), isNull(), isNull(), eq(true)))
                    .thenReturn(pageResult);

            mockMvc.perform(get("/api/networks/net-1/relationships?mirror=true").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            verify(service).listNetworkRelationships("net-1", 0, 50, null, null, true);
        }
    }

    @Nested
    @DisplayName("POST /api/networks/{networkId}/relationships")
    class CreateRelationshipTests {
        @Test
        @DisplayName("creates relationship with mirror=true by default")
        void createsRelationshipWithMirror() throws Exception {
            RelationshipView created = new RelationshipView(
                    "rel-1", "mirror-1", "rel-1-rev", "agent-1", "agent-2", 1.0, 0.5, "FRIEND", "DIRECT");
            when(service.createRelationship(eq("net-1"), any(), eq(true))).thenReturn(created);

            mockMvc.perform(post("/api/networks/net-1/relationships")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"sourceAgentId\":\"agent-1\",\"targetAgentId\":\"agent-2\","
                                    + "\"weight\":1.0,\"trustValue\":0.5,\"relationshipType\":\"FRIEND\","
                                    + "\"transmissionMode\":\"DIRECT\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.relationshipId").value("rel-1"));
        }

        @Test
        @DisplayName("creates unidirectional relationship when mirror=false")
        void createsUnidirectionalRelationship() throws Exception {
            RelationshipView created =
                    new RelationshipView("rel-1", null, null, "agent-1", "agent-2", 1.0, 0.5, "FRIEND", "DIRECT");
            when(service.createRelationship(eq("net-1"), any(), eq(false))).thenReturn(created);

            mockMvc.perform(post("/api/networks/net-1/relationships?mirror=false")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"sourceAgentId\":\"agent-1\",\"targetAgentId\":\"agent-2\","
                                    + "\"weight\":1.0,\"trustValue\":0.5,\"relationshipType\":\"FRIEND\","
                                    + "\"transmissionMode\":\"DIRECT\"}"))
                    .andExpect(status().isCreated());

            verify(service).createRelationship(eq("net-1"), any(), eq(false));
        }

        @Test
        @DisplayName("returns 400 when required fields are missing")
        void validatesRequiredFields() throws Exception {
            mockMvc.perform(post("/api/networks/net-1/relationships")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when self-relationship is attempted")
        void preventsSelfRelationships() throws Exception {
            when(service.createRelationship(eq("net-1"), any(), anyBoolean()))
                    .thenThrow(new com.whispernetwork.api.application.error.BadRequestException(
                            "Self-relationships not allowed"));

            mockMvc.perform(post("/api/networks/net-1/relationships")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"sourceAgentId\":\"agent-1\",\"targetAgentId\":\"agent-1\","
                                    + "\"weight\":1.0,\"trustValue\":0.5,\"relationshipType\":\"FRIEND\","
                                    + "\"transmissionMode\":\"DIRECT\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE /api/networks/{networkId}/relationships/{relationshipId}")
    class DeleteRelationshipTests {
        @Test
        @DisplayName("deletes relationship and returns 204")
        void deletesRelationship() throws Exception {
            mockMvc.perform(delete("/api/networks/net-1/relationships/rel-1")).andExpect(status().isNoContent());

            verify(service).deleteRelationship("net-1", "rel-1");
        }

        @Test
        @DisplayName("returns 404 when relationship not found")
        void returnsNotFoundWhenRelationshipNotFound() throws Exception {
            doThrow(new com.whispernetwork.api.application.error.NotFoundException("Relationship not found"))
                    .when(service)
                    .deleteRelationship("net-1", "unknown");

            mockMvc.perform(delete("/api/networks/net-1/relationships/unknown")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/networks/{networkId}/versions")
    class CreateVersionTests {
        @Test
        @DisplayName("creates version and returns 201")
        void createsVersion() throws Exception {
            NetworkVersionView created = new NetworkVersionView("net-1", 1, "Version 1", "owner", Instant.now());
            when(service.createVersion(eq("net-1"), any())).thenReturn(created);

            mockMvc.perform(post("/api/networks/net-1/versions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"description\":\"Version 1\",\"agents\":[],\"relationships\":[]}"))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(jsonPath("$.versionNumber").value(1));
        }
    }

    @Nested
    @DisplayName("GET /api/networks/{networkId}/stream")
    class StreamNetworkTests {
        @Test
        @DisplayName("returns SSE emitter for valid network")
        void streamNetworSuccess() throws Exception {
            SseEmitter emitter = new SseEmitter();
            when(actorContext.currentActorId()).thenReturn("owner-1");
            when(service.getDraftSnapshotJson("net-1")).thenReturn("{\"agents\":[]}");
            when(broadcaster.subscribe("owner-1", "net-1", null)).thenReturn(emitter);
            when(broadcaster.isValidLastEventId(null)).thenReturn(true);

            mockMvc.perform(get("/api/networks/net-1/stream").accept("text/event-stream"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("validates Last-Event-ID header")
        void validatesLastEventId() throws Exception {
            SseEmitter emitter = new SseEmitter();
            when(actorContext.currentActorId()).thenReturn("owner-1");
            when(service.getDraftSnapshotJson("net-1")).thenReturn("{\"agents\":[]}");
            when(broadcaster.subscribe("owner-1", "net-1", "invalid")).thenReturn(emitter);
            when(broadcaster.isValidLastEventId("invalid")).thenReturn(false);

            mockMvc.perform(get("/api/networks/net-1/stream")
                            .header("Last-Event-ID", "invalid")
                            .accept("text/event-stream"))
                    .andExpect(status().isOk());

            verify(broadcaster).isValidLastEventId("invalid");
        }
    }
}
