package com.whispernetwork.api.interfaces.http.controller;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.whispernetwork.api.application.dto.catalog.AgentCommand;
import com.whispernetwork.api.application.dto.catalog.AgentView;
import com.whispernetwork.api.application.dto.catalog.PageResult;
import com.whispernetwork.api.application.services.catalog.AgentCatalogApplicationService;
import com.whispernetwork.api.interfaces.http.error.ApiExceptionHandler;
import com.whispernetwork.api.interfaces.http.mapper.AgentHttpMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AgentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ApiExceptionHandler.class, AgentHttpMapper.class})
class AgentControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AgentCatalogApplicationService service;

    @Nested
    @DisplayName("GET /api/agents")
    class ListAgentsTests {
        @Test
        @DisplayName("returns paginated list of agents")
        void listAgents() throws Exception {
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
            when(service.listAgents(eq(0), eq(50), isNull(), isNull())).thenReturn(pageResult);

            mockMvc.perform(get("/api/agents").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items", hasSize(1)))
                    .andExpect(jsonPath("$.items[0].agentId").value("agent-1"))
                    .andExpect(jsonPath("$.items[0].nickname").value("alpha"))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(50))
                    .andExpect(jsonPath("$.total").value(1));
        }

        @Test
        @DisplayName("supports custom page and size parameters")
        void supportsCustomPagination() throws Exception {
            PageResult<AgentView> pageResult = new PageResult<>(List.of(), 1, 25, 100);
            when(service.listAgents(eq(1), eq(25), isNull(), isNull())).thenReturn(pageResult);

            mockMvc.perform(get("/api/agents?page=1&size=25").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page").value(1))
                    .andExpect(jsonPath("$.size").value(25));
        }

        @Test
        @DisplayName("enforces maximum page size of 100")
        void enforcesMaxPageSize() throws Exception {
            when(service.listAgents(eq(0), eq(101), isNull(), isNull()))
                    .thenThrow(new IllegalArgumentException("size must be between 1 and 100"));

            mockMvc.perform(get("/api/agents?size=101").accept(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.detail").value("size must be between 1 and 100"));
        }

        @Test
        @DisplayName("validates page parameter is non-negative")
        void validatesPageNonNegative() throws Exception {
            when(service.listAgents(eq(-1), eq(50), isNull(), isNull()))
                    .thenThrow(new IllegalArgumentException("page must be >= 0"));

            mockMvc.perform(get("/api/agents?page=-1").accept(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("page must be >= 0"));
        }

        @Test
        @DisplayName("filters by deployed status")
        void filtersByDeployedStatus() throws Exception {
            PageResult<AgentView> pageResult = new PageResult<>(List.of(), 0, 50, 0);
            when(service.listAgents(eq(0), eq(50), eq(true), eq("net-1"))).thenReturn(pageResult);

            mockMvc.perform(get("/api/agents?deployed=true&networkId=net-1").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            verify(service).listAgents(0, 50, true, "net-1");
        }

        @Test
        @DisplayName("returns error when deployed filter used without networkId")
        void requiresNetworkIdWithDeployedFilter() throws Exception {
            when(service.listAgents(eq(0), eq(50), eq(true), isNull()))
                    .thenThrow(new IllegalArgumentException("networkId is required when deployed filter is provided"));

            mockMvc.perform(get("/api/agents?deployed=true").accept(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/agents")
    class CreateAgentTests {
        @Test
        @DisplayName("creates agent and returns 201 with Location header")
        void createsAgent() throws Exception {
            AgentView created = new AgentView(
                    "agent-auto",
                    "Test Agent",
                    "NORMAL",
                    0.15,
                    0.25,
                    0.35,
                    0.45,
                    Instant.now(),
                    Instant.now(),
                    "owner",
                    "owner");
            when(service.createAgent(Mockito.any(AgentCommand.class))).thenReturn(created);

            mockMvc.perform(
                            post("/api/agents")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            "{\"nickname\":\"Test Agent\",\"role\":\"NORMAL\","
                                                    + "\"bias\":0.15,\"stubbornness\":0.25,\"susceptibility\":0.35,\"suspiciousness\":0.45}"))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(header().string("Location", containsString("/api/agents/agent-auto")))
                    .andExpect(jsonPath("$.nickname").value("Test Agent"))
                    .andExpect(jsonPath("$.bias").value(0.15));
        }

        @Test
        @DisplayName("creates agent with provided ID")
        void createsAgentWithProvidedId() throws Exception {
            AgentView created = new AgentView(
                    "custom-agent",
                    "Test Agent",
                    "NORMAL",
                    0.1,
                    0.2,
                    0.3,
                    0.4,
                    Instant.now(),
                    Instant.now(),
                    "owner",
                    "owner");
            when(service.createAgent(Mockito.any(AgentCommand.class))).thenReturn(created);

            mockMvc.perform(
                            post("/api/agents")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            "{\"agentId\":\"custom-agent\",\"nickname\":\"Test Agent\",\"role\":\"NORMAL\","
                                                    + "\"bias\":0.1,\"stubbornness\":0.2,\"susceptibility\":0.3,\"suspiciousness\":0.4}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.agentId").value("custom-agent"));
        }

        @Test
        @DisplayName("validates personality parameter ranges")
        void validatesPersonalityParameterRanges() throws Exception {
            mockMvc.perform(
                            post("/api/agents")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            "{\"nickname\":\"Test Agent\",\"role\":\"NORMAL\","
                                                    + "\"bias\":1.5,\"stubbornness\":0.2,\"susceptibility\":0.3,\"suspiciousness\":0.4}"))
                    .andExpect(status().isBadRequest());

            mockMvc.perform(
                            post("/api/agents")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            "{\"nickname\":\"Test Agent\",\"role\":\"NORMAL\","
                                                    + "\"bias\":0.1,\"stubbornness\":0.2,\"susceptibility\":0.3,\"suspiciousness\":-0.1}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("validates required fields")
        void validatesRequiredFields() throws Exception {
            mockMvc.perform(post("/api/agents")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nickname\":\"Test Agent\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/agents/{agentId}")
    class GetAgentTests {
        @Test
        @DisplayName("returns agent by ID")
        void getsAgent() throws Exception {
            AgentView agent = new AgentView(
                    "agent-1",
                    "Test Agent",
                    "NORMAL",
                    0.1,
                    0.2,
                    0.3,
                    0.4,
                    Instant.EPOCH,
                    Instant.EPOCH,
                    "owner",
                    "owner");
            when(service.findAgent("agent-1")).thenReturn(Optional.of(agent));

            mockMvc.perform(get("/api/agents/agent-1").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.agentId").value("agent-1"))
                    .andExpect(jsonPath("$.nickname").value("Test Agent"));
        }

        @Test
        @DisplayName("returns 404 when agent not found")
        void returnsNotFoundWhenAgentNotFound() throws Exception {
            when(service.findAgent("unknown")).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/agents/unknown").accept(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }

    @Nested
    @DisplayName("PUT /api/agents/{agentId}")
    class UpdateAgentTests {
        @Test
        @DisplayName("updates agent and returns 200")
        void updatesAgent() throws Exception {
            AgentView updated = new AgentView(
                    "agent-1",
                    "Updated Agent",
                    "NORMAL",
                    0.15,
                    0.25,
                    0.35,
                    0.45,
                    Instant.EPOCH,
                    Instant.now(),
                    "owner",
                    "owner");
            when(service.updateAgent(eq("agent-1"), Mockito.any(AgentCommand.class)))
                    .thenReturn(Optional.of(updated));

            mockMvc.perform(
                            put("/api/agents/agent-1")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            "{\"nickname\":\"Updated Agent\",\"role\":\"NORMAL\","
                                                    + "\"bias\":0.15,\"stubbornness\":0.25,\"susceptibility\":0.35,\"suspiciousness\":0.45}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nickname").value("Updated Agent"))
                    .andExpect(jsonPath("$.bias").value(0.15));
        }

        @Test
        @DisplayName("returns 404 when agent not found during update")
        void returnsNotFoundWhenAgentNotFoundForUpdate() throws Exception {
            when(service.updateAgent(eq("unknown"), Mockito.any(AgentCommand.class)))
                    .thenReturn(Optional.empty());

            mockMvc.perform(
                            put("/api/agents/unknown")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            "{\"nickname\":\"Updated\",\"role\":\"NORMAL\","
                                                    + "\"bias\":0.1,\"stubbornness\":0.2,\"susceptibility\":0.3,\"suspiciousness\":0.4}"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("validates update request body")
        void validatesUpdateRequestBody() throws Exception {
            mockMvc.perform(put("/api/agents/agent-1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {
        @Test
        @DisplayName("includes request ID in error responses")
        void includesRequestIdInErrors() throws Exception {
            when(service.findAgent("unknown")).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/agents/unknown").accept(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(header().exists("X-Request-Id"))
                    .andExpect(jsonPath("$.requestId").isNotEmpty());
        }

        @Test
        @DisplayName("returns RFC 7807 Problem Detail format")
        void returnsProblemDetailFormat() throws Exception {
            when(service.listAgents(eq(0), eq(101), isNull(), isNull()))
                    .thenThrow(new IllegalArgumentException("size must be between 1 and 100"));

            mockMvc.perform(get("/api/agents?size=101").accept(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").exists())
                    .andExpect(jsonPath("$.title").exists())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.detail").exists())
                    .andExpect(jsonPath("$.instance").exists());
        }
    }
}
