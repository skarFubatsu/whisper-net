package com.whispernetwork.api.interfaces.http.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.whispernetwork.api.application.dto.SimulationTimelineEventSnapshot;
import com.whispernetwork.api.application.ports.out.SimulationEventStreamPort;
import com.whispernetwork.api.application.security.ActorContext;
import com.whispernetwork.api.application.services.simulation.SimulationCommandApplicationService;
import com.whispernetwork.api.application.services.simulation.SimulationQueryApplicationService;
import com.whispernetwork.api.interfaces.http.error.ApiExceptionHandler;
import com.whispernetwork.api.interfaces.http.mapper.SimulationHttpMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@WebMvcTest(SimulationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ApiExceptionHandler.class, SimulationHttpMapper.class})
@SuppressWarnings("nullness")
class SimulationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SimulationCommandApplicationService commandService;

    @MockBean
    private SimulationQueryApplicationService queryService;

    @MockBean
    private SimulationEventStreamPort eventStream;

    @MockBean
    private ActorContext actorContext;

    @Nested
    @DisplayName("GET /api/simulations/{runId}/stream")
    class StreamSimulationEventsTests {

        @Test
        @DisplayName("opens SSE stream with owner-scoped replay")
        void opensSseStreamWithReplay() throws Exception {
            SimulationTimelineEventSnapshot replayEvent = new SimulationTimelineEventSnapshot(
                    "STARTED",
                    "run-1",
                    "net-1",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    Instant.EPOCH);

            when(actorContext.currentActorId()).thenReturn("owner-1");
            when(queryService.listTimeline("run-1")).thenReturn(List.of(replayEvent));
            when(eventStream.open("owner-1", "run-1", List.of(replayEvent))).thenReturn(new SseEmitter(0L));

            mockMvc.perform(get("/api/simulations/run-1/stream").accept(MediaType.TEXT_EVENT_STREAM))
                    .andExpect(status().isOk())
                    .andExpect(request().asyncStarted());

            verify(queryService).listTimeline("run-1");
            verify(eventStream).open("owner-1", "run-1", List.of(replayEvent));
        }
    }

    @Nested
    @DisplayName("GET /api/simulations/{runId}/events")
    class ListSimulationEventsTests {

        @Test
        @DisplayName("returns timeline events payload")
        void returnsTimelineEventsPayload() throws Exception {
            SimulationTimelineEventSnapshot event = new SimulationTimelineEventSnapshot(
                    "TICK_COMPLETED",
                    "run-1",
                    "net-1",
                    3,
                    12,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    List.of("agent-1"),
                    null,
                    null,
                    null,
                    Instant.EPOCH);

            when(queryService.listTimeline("run-1")).thenReturn(List.of(event));

            mockMvc.perform(get("/api/simulations/run-1/events").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].eventType").value("TICK_COMPLETED"))
                    .andExpect(jsonPath("$[0].tickNumber").value(3))
                    .andExpect(jsonPath("$[0].updatedAgents").value(12));
        }
    }
}
