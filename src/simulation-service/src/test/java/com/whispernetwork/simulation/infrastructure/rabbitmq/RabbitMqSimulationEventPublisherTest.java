package com.whispernetwork.simulation.infrastructure.rabbitmq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rabbitmq.client.Channel;
import com.whispernetwork.shared.messaging.RabbitTopology;
import com.whispernetwork.schema.v1.events.SimulationStartedEvent;
import com.whispernetwork.simulation.application.model.SimulationEvent;
import com.whispernetwork.simulation.core.engine.AgentOpinionUpdate;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class RabbitMqSimulationEventPublisherTest {

  @Test
  void shouldEnablePublisherConfirmsOnConstruction() throws Exception {
    Channel channel = Mockito.mock(Channel.class);

    new RabbitMqSimulationEventPublisher(channel);

    verify(channel, times(1)).confirmSelect();
  }

  @Test
  void shouldThrowWhenConfirmModeCannotBeEnabled() throws Exception {
    Channel channel = Mockito.mock(Channel.class);
    Mockito.doThrow(new RuntimeException("confirm init failed")).when(channel).confirmSelect();

    assertThrows(IllegalStateException.class, () -> new RabbitMqSimulationEventPublisher(channel));
  }

  @Test
  void shouldPublishStartedEventAndWaitForConfirm() throws Exception {
    Channel channel = Mockito.mock(Channel.class);
    when(channel.waitForConfirms(anyLong())).thenReturn(true);
    RabbitMqSimulationEventPublisher publisher = new RabbitMqSimulationEventPublisher(channel);

    publisher.publish(new SimulationEvent.SimulationStarted(
        "run-1",
        "network-1",
        3,
        "actor-1",
        "request-1",
        Instant.ofEpochMilli(1234L)));

    ArgumentCaptor<byte[]> payloadCaptor = ArgumentCaptor.forClass(byte[].class);
    verify(channel).basicPublish(
        eq(RabbitTopology.EVENTS_EXCHANGE),
        eq(RabbitTopology.EVENT_ROUTING_SIMULATION_STARTED),
        eq(true),
        eq(null),
        payloadCaptor.capture());
    verify(channel).waitForConfirms(eq(5000L));

    SimulationStartedEvent event = SimulationStartedEvent.parseFrom(payloadCaptor.getValue());
    assertEquals("run-1", event.getSimulationRunId());
    assertEquals("network-1", event.getNetworkId());
    assertEquals(3, event.getNetworkVersionNumber());
    assertEquals("actor-1", event.getInitiatedByActorId());
    assertEquals("request-1", event.getClientRequestId());
    assertEquals(1234L, event.getStartedAtEpochMillis());
  }

  @Test
  void shouldPublishTickAndOpinionUpdates() throws Exception {
    Channel channel = Mockito.mock(Channel.class);
    when(channel.waitForConfirms(anyLong())).thenReturn(true);
    RabbitMqSimulationEventPublisher publisher = new RabbitMqSimulationEventPublisher(channel);

    SimulationEvent.SimulationTickCompleted tick = new SimulationEvent.SimulationTickCompleted(
        "run-2",
        "network-2",
        9,
        2,
        List.of(
            new AgentOpinionUpdate("agent-a", 0.1, 0.2, 0.3, false, null, false, List.of("source-a")),
            new AgentOpinionUpdate("agent-b", 0.4, 0.5, 0.6, true, "origin-b", true, List.of("source-b"))),
        Instant.ofEpochMilli(5678L));

    publisher.publish(tick);

    verify(channel, times(3)).basicPublish(
        eq(RabbitTopology.EVENTS_EXCHANGE),
        any(),
        eq(true),
        eq(null),
        any(byte[].class));
    verify(channel, times(3)).waitForConfirms(eq(5000L));
  }

  @Test
  void shouldThrowWhenPublishNotConfirmed() throws Exception {
    Channel channel = Mockito.mock(Channel.class);
    when(channel.waitForConfirms(anyLong())).thenReturn(false);
    RabbitMqSimulationEventPublisher publisher = new RabbitMqSimulationEventPublisher(channel);

    assertThrows(IllegalStateException.class, () -> publisher.publish(new SimulationEvent.SimulationCompleted(
        "run-3",
        "network-3",
        1,
        Instant.now())));
  }

  @Test
  void shouldWrapChannelPublishException() throws Exception {
    Channel channel = Mockito.mock(Channel.class);
    Mockito.doThrow(new RuntimeException("publish failed"))
        .when(channel)
        .basicPublish(eq(RabbitTopology.EVENTS_EXCHANGE), any(), anyBoolean(), eq(null), any(byte[].class));
    RabbitMqSimulationEventPublisher publisher = new RabbitMqSimulationEventPublisher(channel);

    assertThrows(IllegalStateException.class, () -> publisher.publish(new SimulationEvent.SimulationFailed(
        "run-4",
        "network-4",
        "boom",
        Instant.now())));
  }
}
