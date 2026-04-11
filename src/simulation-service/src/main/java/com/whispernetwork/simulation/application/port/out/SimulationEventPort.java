package com.whispernetwork.simulation.application.port.out;

import com.whispernetwork.simulation.application.model.SimulationEvent;

/**
 * Event publishing boundary.
 */
public interface SimulationEventPort {

  /**
   * Publishes an application event.
   *
   * @param event simulation event
   */
  void publish(SimulationEvent event);
}
