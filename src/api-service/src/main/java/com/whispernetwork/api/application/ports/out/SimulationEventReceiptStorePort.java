package com.whispernetwork.api.application.ports.out;

import com.whispernetwork.api.application.dto.SimulationEventReceipt;

public interface SimulationEventReceiptStorePort {
    void saveAndFlush(SimulationEventReceipt simulationEventReceipt);
}
