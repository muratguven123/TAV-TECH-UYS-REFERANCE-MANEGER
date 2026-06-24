package com.tav.referance_manager.events;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class ReferenceEventPublisherTest {

    @Mock ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks ReferenceEventPublisher publisher;

    @ParameterizedTest(name = "publish({0}, {1}) doğru alanlarla yayımlar")
    @CsvSource({
            "AIRLINE,  CREATED, TK",
            "AIRLINE,  UPDATED, TK",
            "AIRLINE,  DELETED, TK",
            "STATION,  CREATED, LTFM",
            "STATION,  UPDATED, LTFM",
            "STATION,  DELETED, LTFM",
            "AIRCRAFT, CREATED, TC-JFA",
            "AIRCRAFT, UPDATED, TC-JFA",
            "AIRCRAFT, DELETED, TC-JFA",
            "ROUTE,    CREATED, LTFM-LTAC",
            "ROUTE,    UPDATED, LTFM-LTAC",
            "ROUTE,    DELETED, LTFM-LTAC"
    })
    @DisplayName("4 entity × 3 change-type matrisi: doğru ReferenceChangedEvent publish edilir")
    void publish_emitsCorrectEventForAllCombinations(
            ReferenceEntityType entityType,
            ChangeType changeType,
            String businessKey) {
        // given
        Object payload = changeType == ChangeType.DELETED ? null : new Object();

        // when
        publisher.publish(entityType, changeType, businessKey, payload);

        // then
        ArgumentCaptor<ReferenceChangedEvent> cap = ArgumentCaptor.forClass(ReferenceChangedEvent.class);
        verify(applicationEventPublisher).publishEvent(cap.capture());
        ReferenceChangedEvent event = cap.getValue();
        assertThat(event.entityType()).isEqualTo(entityType);
        assertThat(event.changeType()).isEqualTo(changeType);
        assertThat(event.businessKey()).isEqualTo(businessKey);
        assertThat(event.payload()).isSameAs(payload);
        verifyNoMoreInteractions(applicationEventPublisher);
    }

    @Test
    @DisplayName("publish → partitionKey 'ENTITY:key' formatında üretilir")
    void publish_partitionKeyFormat() {
        publisher.publish(ReferenceEntityType.AIRLINE, ChangeType.CREATED, "TK", new Object());
        ArgumentCaptor<ReferenceChangedEvent> cap = ArgumentCaptor.forClass(ReferenceChangedEvent.class);
        verify(applicationEventPublisher).publishEvent(cap.capture());
        assertThat(cap.getValue().partitionKey()).isEqualTo("AIRLINE:TK");
    }
}
