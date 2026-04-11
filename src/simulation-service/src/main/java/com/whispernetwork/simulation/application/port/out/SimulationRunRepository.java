package com.whispernetwork.simulation.application.port.out;

import com.whispernetwork.simulation.application.model.IdempotencyKey;
import com.whispernetwork.simulation.application.model.SimulationRun;
import java.util.Optional;

/**
 * Repository boundary for simulation run state.
 */
public interface SimulationRunRepository {

  /**
   * Saves or updates a run.
   */
  void save(SimulationRun run);

  /**
   * Finds a run by id.
   */
  Optional<SimulationRun> findById(String runId);

  /**
   * Finds the active run for a network, if any.
   */
  Optional<SimulationRun> findActiveByNetwork(String networkId);

  /**
   * Stores idempotency mapping to run id.
   */
  void putIdempotency(IdempotencyKey key, String runId);

  /**
   * Finds existing run id for idempotency key.
   */
  Optional<String> findByIdempotency(IdempotencyKey key);
}
