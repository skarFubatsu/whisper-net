package com.whispernetwork.api.application.services.simulation;

import com.whispernetwork.api.application.dto.SimulationEventReceipt;
import com.whispernetwork.api.application.ports.out.SimulationEventReceiptStorePort;
import java.time.Instant;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Deduplicates RabbitMQ simulation events by event id.
 */
@Service
public class SimulationEventReceiptApplicationService {
    private final SimulationEventReceiptStorePort receiptStore;

    public SimulationEventReceiptApplicationService(SimulationEventReceiptStorePort receiptStore) {
        this.receiptStore = receiptStore;
    }

    public boolean markReceivedIfNew(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            return true;
        }

        try {
            receiptStore.saveAndFlush(new SimulationEventReceipt(eventId, Instant.now()));
            return true;
        } catch (DataIntegrityViolationException ex) {
            return false;
        }
    }
}
