package com.whispernetwork.simulation.infrastructure.rabbitmq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.whispernetwork.schema.v1.commands.SimulationCancelRequestedCommand;
import com.whispernetwork.schema.v1.commands.SimulationRequestedCommand;
import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("RabbitMqSimulationCommandWorker")
class RabbitMqSimulationCommandWorkerTest {

    @Nested
    @DisplayName("Requested Command Parsing")
    class RequestedParsing {

        @Test
        void shouldParseSimulationRequestedPayload() throws Exception {
            byte[] payload = SimulationRequestedCommand.newBuilder()
                    .setNetworkId("network-1")
                    .setNetworkVersionNumber(2)
                    .setRequestedByActorId("actor-1")
                    .setClientRequestId("request-1")
                    .setRequestedTicks(5)
                    .setEventId("event-1")
                    .build()
                    .toByteArray();

            SimulationRequestedCommand parsed = invokeParseRequested(payload);

            assertEquals("network-1", parsed.getNetworkId());
            assertEquals(2, parsed.getNetworkVersionNumber());
            assertEquals("actor-1", parsed.getRequestedByActorId());
            assertEquals("request-1", parsed.getClientRequestId());
            assertEquals(5, parsed.getRequestedTicks());
            assertEquals("event-1", parsed.getEventId());
        }

        @Test
        void shouldRejectInvalidSimulationRequestedPayload() {
            assertThrows(IllegalArgumentException.class, () -> invokeParseRequested(new byte[] {1, 2, 3}));
        }
    }

    @Nested
    @DisplayName("Cancel Command Parsing")
    class CancelParsing {

        @Test
        void shouldParseSimulationCancelPayload() throws Exception {
            byte[] payload = SimulationCancelRequestedCommand.newBuilder()
                    .setSimulationRunId("run-1")
                    .setNetworkId("network-1")
                    .setRequestedByActorId("actor-1")
                    .setClientRequestId("cancel-1")
                    .setEventId("event-2")
                    .build()
                    .toByteArray();

            SimulationCancelRequestedCommand parsed = invokeParseCancel(payload);

            assertEquals("run-1", parsed.getSimulationRunId());
            assertEquals("network-1", parsed.getNetworkId());
            assertEquals("actor-1", parsed.getRequestedByActorId());
            assertEquals("cancel-1", parsed.getClientRequestId());
            assertEquals("event-2", parsed.getEventId());
        }

        @Test
        void shouldRejectInvalidSimulationCancelPayload() {
            assertThrows(IllegalArgumentException.class, () -> invokeParseCancel(new byte[] {4, 5, 6}));
        }
    }

    private static SimulationRequestedCommand invokeParseRequested(byte[] payload) throws Exception {
        Method method = RabbitMqSimulationCommandWorker.class.getDeclaredMethod("parseRequestedCommand", byte[].class);
        method.setAccessible(true);
        try {
            return (SimulationRequestedCommand) method.invoke(null, payload);
        } catch (Exception ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof IllegalArgumentException illegalArgumentException) {
                throw illegalArgumentException;
            }
            throw ex;
        }
    }

    private static SimulationCancelRequestedCommand invokeParseCancel(byte[] payload) throws Exception {
        Method method = RabbitMqSimulationCommandWorker.class.getDeclaredMethod("parseCancelCommand", byte[].class);
        method.setAccessible(true);
        try {
            return (SimulationCancelRequestedCommand) method.invoke(null, payload);
        } catch (Exception ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof IllegalArgumentException illegalArgumentException) {
                throw illegalArgumentException;
            }
            throw ex;
        }
    }
}
