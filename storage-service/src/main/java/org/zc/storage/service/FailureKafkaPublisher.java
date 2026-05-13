package org.zc.storage.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.zc.common.Event;
import org.zc.common.FailedEventMessage;
import org.zc.storage.config.EventProperties;

@Component
public class FailureKafkaPublisher implements FailurePublisher {

    private static final Logger log = LoggerFactory.getLogger(FailureKafkaPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final EventProperties eventProperties;

    public FailureKafkaPublisher(KafkaTemplate<String, Object> kafkaTemplate, EventProperties eventProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.eventProperties = eventProperties;
    }

    @Override
    public void publish(Event event, String stage, String reason, int attempts) {
        FailedEventMessage message = new FailedEventMessage();
        message.setEvent(event);
        message.setStage(stage);
        message.setReason(reason);
        message.setAttempts(attempts);
        message.setFailedAt(System.currentTimeMillis());

        String messageKey = event == null ? "unknown-device" : event.getDeviceId();
        kafkaTemplate.send(eventProperties.getFailureTopic(), messageKey, message);
        log.warn("Published storage failure message. topic={}, eventId={}, stage={}, attempts={}",
                eventProperties.getFailureTopic(), event == null ? null : event.getEventId(), stage, attempts);
    }
}
