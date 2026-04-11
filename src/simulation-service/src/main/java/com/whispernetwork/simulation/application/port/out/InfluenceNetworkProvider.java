package com.whispernetwork.simulation.application.port.out;

import com.whispernetwork.simulation.core.model.InfluenceNetwork;

/**
 * Provides immutable network snapshots for simulation runs.
 */
public interface InfluenceNetworkProvider {

  /**
   * Loads a network snapshot by id and version.
   *
   * @param networkId network id
   * @param networkVersionNumber version number
   * @return deep-copyable network snapshot
   */
  InfluenceNetwork loadNetworkSnapshot(String networkId, int networkVersionNumber);
}
