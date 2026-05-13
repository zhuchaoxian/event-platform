package org.zc.consumer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.zc.common.Event;

@Service
public class EventProcessingService {

    private static final Logger log = LoggerFactory.getLogger(EventProcessingService.class);

    private final EventStoragePublisher eventStoragePublisher;

    public EventProcessingService(EventStoragePublisher eventStoragePublisher) {
        this.eventStoragePublisher = eventStoragePublisher;
    }

    public void process(Event event) {
        validate(event);
        log.info("Start processing event asynchronously. eventId={}, deviceId={}, type={}",
                event.getEventId(), event.getDeviceId(), event.getType());

        int payloadSize = event.getPayload() == null ? 0 : event.getPayload().size();
        log.info("Rule evaluation placeholder completed. eventId={}, payloadSize={}",
                event.getEventId(), payloadSize);
        eventStoragePublisher.publish(event);
    }

    private void validate(Event event) {
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }
        if (event.getDeviceId() == null || event.getDeviceId().isBlank()) {
            throw new IllegalArgumentException("deviceId must not be blank");
        }
        if (event.getType() == null || event.getType().isBlank()) {
            throw new IllegalArgumentException("type must not be blank");
        }
    }
}
