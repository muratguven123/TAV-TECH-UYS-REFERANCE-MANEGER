package com.tav.referance_manager.events;

import com.tav.uys.events.ChangeType;
import com.tav.uys.events.ReferenceChangedEvent;
import com.tav.uys.events.ReferenceEntityType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReferenceEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public void publish(ReferenceEntityType entityType, ChangeType changeType,
                        String businessKey, Object payload) {
        applicationEventPublisher.publishEvent(
                new ReferenceChangedEvent(entityType, changeType, businessKey, payload)
        );
    }
}
