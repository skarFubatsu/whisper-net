package com.whispernetwork.simulation;

import com.whispernetwork.simulation.infrastructure.rabbitmq.RabbitMqSimulationCommandWorker;
import java.util.concurrent.CountDownLatch;

/**
 * Entry point for the background simulation service process.
 */
public final class SimulationServiceMain {

    private SimulationServiceMain() {}

    /**
     * Starts the background service process.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        try (RabbitMqSimulationCommandWorker worker = new RabbitMqSimulationCommandWorker()) {
            CountDownLatch shutdownLatch = new CountDownLatch(1);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Simulation service shutdown requested.");
                shutdownLatch.countDown();
            }));

            System.out.println("Simulation service worker started and listening for RabbitMQ commands.");
            shutdownLatch.await();
        } catch (Exception ex) {
            System.err.println("Simulation service failed to start: " + ex.getMessage());
            ex.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
