package com.whispernetwork.api.infrastructure.persistence;

import com.whispernetwork.api.application.dto.SimulationEventReceipt;
import com.whispernetwork.api.application.ports.out.SimulationEventReceiptStorePort;
import com.whispernetwork.api.infrastructure.persistence.entity.SimulationEventReceiptEntity;
import com.whispernetwork.api.infrastructure.persistence.repository.SimulationEventReceiptJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class JpaSimulationEventReceiptStore implements SimulationEventReceiptStorePort {

    private final SimulationEventReceiptJpaRepository eventReceiptRepo;

    public JpaSimulationEventReceiptStore(SimulationEventReceiptJpaRepository eventReceiptRepo) {
        this.eventReceiptRepo = eventReceiptRepo;
    }

    @Override
    public void saveAndFlush(SimulationEventReceipt simulationEventReceipt) {
        eventReceiptRepo.saveAndFlush(toEntity(simulationEventReceipt));
    }

    private SimulationEventReceiptEntity toEntity(SimulationEventReceipt eventReceipt) {
        return new SimulationEventReceiptEntity(eventReceipt.eventId(), eventReceipt.receivedAt());
    }
}
