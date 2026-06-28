package com.tav.referance_manager.events;

import com.tav.uys.events.ReferenceChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReferenceEventKafkaRelay {

    static final String TOPIC = "reference.events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReferenceChanged(ReferenceChangedEvent event) {
        String key = event.partitionKey();
        log.debug("Publishing reference event: topic={}, key={}, changeType={}", TOPIC, key, event.changeType());
        kafkaTemplate.send(TOPIC, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish reference event: key={}, error={}", key, ex.getMessage(), ex);
                    } else {
                        log.debug("Reference event published: key={}, offset={}",
                                key, result.getRecordMetadata().offset());
                    }
                });
    }
}
