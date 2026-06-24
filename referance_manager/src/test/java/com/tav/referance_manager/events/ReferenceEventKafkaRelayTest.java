package com.tav.referance_manager.events;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReferenceEventKafkaRelayTest {

    @Mock KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks ReferenceEventKafkaRelay relay;

    @Test
    @DisplayName("onReferenceChanged → reference.events topic'ine partitionKey ile gönderir")
    void onReferenceChanged_sendsToTopicWithPartitionKey() {
        // given
        ReferenceChangedEvent event = new ReferenceChangedEvent(
                ReferenceEntityType.AIRLINE, ChangeType.CREATED, "TK", "payload-obj");
        when(kafkaTemplate.send(any(String.class), any(String.class), any()))
                .thenReturn(CompletableFuture.completedFuture(mockSendResult()));

        // when
        relay.onReferenceChanged(event);

        // then
        ArgumentCaptor<String> topicCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> valueCap = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(topicCap.capture(), keyCap.capture(), valueCap.capture());

        assertThat(topicCap.getValue()).isEqualTo("reference.events");
        assertThat(keyCap.getValue()).isEqualTo("AIRLINE:TK");
        assertThat(valueCap.getValue()).isSameAs(event);
        verifyNoMoreInteractions(kafkaTemplate);
    }

    @Test
    @DisplayName("onReferenceChanged → STATION DELETED için key 'STATION:LTFM'")
    void onReferenceChanged_stationDeletedKey() {
        // given
        ReferenceChangedEvent event = new ReferenceChangedEvent(
                ReferenceEntityType.STATION, ChangeType.DELETED, "LTFM", null);
        when(kafkaTemplate.send(any(String.class), any(String.class), any()))
                .thenReturn(CompletableFuture.completedFuture(mockSendResult()));

        // when
        relay.onReferenceChanged(event);

        // then
        verify(kafkaTemplate).send(eq("reference.events"), eq("STATION:LTFM"), eq(event));
    }

    @Test
    @DisplayName("onReferenceChanged → AIRCRAFT UPDATED için key 'AIRCRAFT:TC-JFA'")
    void onReferenceChanged_aircraftUpdatedKey() {
        // given
        ReferenceChangedEvent event = new ReferenceChangedEvent(
                ReferenceEntityType.AIRCRAFT, ChangeType.UPDATED, "TC-JFA", "x");
        when(kafkaTemplate.send(any(String.class), any(String.class), any()))
                .thenReturn(CompletableFuture.completedFuture(mockSendResult()));

        // when
        relay.onReferenceChanged(event);

        // then
        verify(kafkaTemplate).send(eq("reference.events"), eq("AIRCRAFT:TC-JFA"), eq(event));
    }

    @Test
    @DisplayName("onReferenceChanged → ROUTE CREATED key 'ROUTE:LTFM-LTAC'")
    void onReferenceChanged_routeCreatedKey() {
        // given
        ReferenceChangedEvent event = new ReferenceChangedEvent(
                ReferenceEntityType.ROUTE, ChangeType.CREATED, "LTFM-LTAC", "x");
        when(kafkaTemplate.send(any(String.class), any(String.class), any()))
                .thenReturn(CompletableFuture.completedFuture(mockSendResult()));

        // when
        relay.onReferenceChanged(event);

        // then
        verify(kafkaTemplate).send(eq("reference.events"), eq("ROUTE:LTFM-LTAC"), eq(event));
    }

    @Test
    @DisplayName("onReferenceChanged → Kafka future fail olursa exception propagate etmez (log'lanır)")
    void onReferenceChanged_whenKafkaFails_doesNotPropagate() {
        // given
        ReferenceChangedEvent event = new ReferenceChangedEvent(
                ReferenceEntityType.AIRLINE, ChangeType.CREATED, "TK", "x");
        CompletableFuture<SendResult<String, Object>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("kafka down"));
        when(kafkaTemplate.send(any(String.class), any(String.class), any()))
                .thenReturn(failed);

        // when / then
        assertThatCode(() -> relay.onReferenceChanged(event)).doesNotThrowAnyException();
        verify(kafkaTemplate).send(eq("reference.events"), eq("AIRLINE:TK"), eq(event));
    }

    @Test
    @DisplayName("TOPIC sabiti 'reference.events' olmalı (FlightService FAS sözleşmesi)")
    void topicConstant() {
        assertThat(ReferenceEventKafkaRelay.TOPIC).isEqualTo("reference.events");
    }

    @SuppressWarnings("unchecked")
    private SendResult<String, Object> mockSendResult() {
        return (SendResult<String, Object>) mock(SendResult.class);
    }
}
