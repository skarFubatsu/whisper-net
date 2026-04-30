package com.whispernetwork.api.application.services.simulation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.whispernetwork.api.application.dto.SimulationEventReceipt;
import com.whispernetwork.api.application.ports.out.SimulationEventReceiptStorePort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class SimulationEventReceiptApplicationServiceTest {

    @Mock
    private SimulationEventReceiptStorePort receiptStore;

    @Nested
    @DisplayName("markReceivedIfNew")
    class MarkReceivedIfNewTests {

        @Test
        @DisplayName("returns true and skips store when event id is null")
        void returnsTrueForNullEventId() {
            SimulationEventReceiptApplicationService service =
                    new SimulationEventReceiptApplicationService(receiptStore);

            boolean result = service.markReceivedIfNew(null);

            assertTrue(result);
            verify(receiptStore, never()).saveAndFlush(any(SimulationEventReceipt.class));
        }

        @Test
        @DisplayName("returns true and skips store when event id is blank")
        void returnsTrueForBlankEventId() {
            SimulationEventReceiptApplicationService service =
                    new SimulationEventReceiptApplicationService(receiptStore);

            boolean result = service.markReceivedIfNew("   ");

            assertTrue(result);
            verify(receiptStore, never()).saveAndFlush(any(SimulationEventReceipt.class));
        }

        @Test
        @DisplayName("returns true and persists receipt when event is new")
        void persistsReceiptForNewEvent() {
            SimulationEventReceiptApplicationService service =
                    new SimulationEventReceiptApplicationService(receiptStore);

            boolean result = service.markReceivedIfNew("evt-1");

            assertTrue(result);
            ArgumentCaptor<SimulationEventReceipt> receiptCaptor =
                    ArgumentCaptor.forClass(SimulationEventReceipt.class);
            verify(receiptStore).saveAndFlush(receiptCaptor.capture());
            assertEquals("evt-1", receiptCaptor.getValue().eventId());
            assertNotNull(receiptCaptor.getValue().receivedAt());
        }

        @Test
        @DisplayName("returns false when duplicate receipt causes integrity violation")
        void returnsFalseOnDuplicate() {
            SimulationEventReceiptApplicationService service =
                    new SimulationEventReceiptApplicationService(receiptStore);
            doThrow(new DataIntegrityViolationException("duplicate"))
                    .when(receiptStore)
                    .saveAndFlush(any(SimulationEventReceipt.class));

            boolean result = service.markReceivedIfNew("evt-dup");

            assertFalse(result);
        }
    }
}
