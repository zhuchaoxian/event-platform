package org.zc.consumer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.zc.common.Event;
import org.zc.common.FailedEventMessage;
import org.zc.consumer.config.ConsumerProperties;

@Component
public class EventFailureKafkaPublisher implements EventFailurePublisher {

    private static final Logger log = LoggerFactory.getLogger(EventFailureKafkaPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ConsumerProperties consumerProperties;

    public EventFailureKafkaPublisher(KafkaTemplate<String, Object> kafkaTemplate, ConsumerProperties consumerProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.consumerProperties = consumerProperties;
    }

    @Override
    public void publish(Event event, String stage, String reason, int attempts) {
        FailedEventMessage message = new FailedEventMessage();
        message.setEvent(event);
        message.setStage(stage);
        message.setReason(reason);
        message.setAttempts(attempts);
        message.setFailedAt(System.currentTimeMillis());

        try {
            String messageKey = event == null ? "unknown-device" : event.getDeviceId();
            kafkaTemplate.send(consumerProperties.getFailureTopic(), messageKey, message);
            log.warn("published failed event to mq. topic={}, eventId={}, stage={}, attempts={}",
                consumerProperties.getFailureTopic(), event == null ? null : event.getEventId(), stage, attempts);
        } catch (Exception exception) {
            log.error("failed to publish event to mq. topic={}, eventId={}, stage={}",
                consumerProperties.getFailureTopic(), event == null ? null : event.getEventId(), stage, exception);
        }
    }
}
