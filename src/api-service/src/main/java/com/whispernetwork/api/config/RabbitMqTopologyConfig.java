package com.whispernetwork.api.config;

import com.whispernetwork.shared.messaging.RabbitTopology;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares RabbitMQ topology used by api-service for simulation orchestration.
 */
@Configuration
public class RabbitMqTopologyConfig {
    public static final String API_QUEUE_SIMULATION_STARTED = "api.simulation.started";
    public static final String API_QUEUE_SIMULATION_CANCELLED = "api.simulation.cancelled";
    public static final String API_QUEUE_SIMULATION_TICK_COMPLETED = "api.simulation.tick.completed";
    public static final String API_QUEUE_SIMULATION_COMPLETED = "api.simulation.completed";
    public static final String API_QUEUE_SIMULATION_FAILED = "api.simulation.failed";
    public static final String API_QUEUE_OPINION_UPDATED = "api.simulation.opinion.updated";

    @Bean
    public DirectExchange commandsExchange() {
        return new DirectExchange(RabbitTopology.COMMANDS_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange eventsExchange() {
        return new DirectExchange(RabbitTopology.EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public Queue apiSimulationStartedQueue() {
        return new Queue(API_QUEUE_SIMULATION_STARTED, true);
    }

    @Bean
    public Queue apiSimulationCancelledQueue() {
        return new Queue(API_QUEUE_SIMULATION_CANCELLED, true);
    }

    @Bean
    public Queue apiSimulationTickCompletedQueue() {
        return new Queue(API_QUEUE_SIMULATION_TICK_COMPLETED, true);
    }

    @Bean
    public Queue apiSimulationCompletedQueue() {
        return new Queue(API_QUEUE_SIMULATION_COMPLETED, true);
    }

    @Bean
    public Queue apiSimulationFailedQueue() {
        return new Queue(API_QUEUE_SIMULATION_FAILED, true);
    }

    @Bean
    public Queue apiOpinionUpdatedQueue() {
        return new Queue(API_QUEUE_OPINION_UPDATED, true);
    }

    @Bean
    public Binding apiStartedBinding(Queue apiSimulationStartedQueue, DirectExchange eventsExchange) {
        return BindingBuilder.bind(apiSimulationStartedQueue)
                .to(eventsExchange)
                .with(RabbitTopology.EVENT_ROUTING_SIMULATION_STARTED);
    }

    @Bean
    public Binding apiCancelledBinding(Queue apiSimulationCancelledQueue, DirectExchange eventsExchange) {
        return BindingBuilder.bind(apiSimulationCancelledQueue)
                .to(eventsExchange)
                .with(RabbitTopology.EVENT_ROUTING_SIMULATION_CANCELLED);
    }

    @Bean
    public Binding apiTickCompletedBinding(Queue apiSimulationTickCompletedQueue, DirectExchange eventsExchange) {
        return BindingBuilder.bind(apiSimulationTickCompletedQueue)
                .to(eventsExchange)
                .with(RabbitTopology.EVENT_ROUTING_SIMULATION_TICK_COMPLETED);
    }

    @Bean
    public Binding apiCompletedBinding(Queue apiSimulationCompletedQueue, DirectExchange eventsExchange) {
        return BindingBuilder.bind(apiSimulationCompletedQueue)
                .to(eventsExchange)
                .with(RabbitTopology.EVENT_ROUTING_SIMULATION_COMPLETED);
    }

    @Bean
    public Binding apiFailedBinding(Queue apiSimulationFailedQueue, DirectExchange eventsExchange) {
        return BindingBuilder.bind(apiSimulationFailedQueue)
                .to(eventsExchange)
                .with(RabbitTopology.EVENT_ROUTING_SIMULATION_FAILED);
    }

    @Bean
    public Binding apiOpinionUpdatedBinding(Queue apiOpinionUpdatedQueue, DirectExchange eventsExchange) {
        return BindingBuilder.bind(apiOpinionUpdatedQueue)
                .to(eventsExchange)
                .with(RabbitTopology.EVENT_ROUTING_OPINION_UPDATED);
    }
}
