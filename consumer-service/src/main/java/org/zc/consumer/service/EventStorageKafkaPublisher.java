package org.zc.consumer.service;

import java.util.concurrent.CompletionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.zc.common.Event;
import org.zc.consumer.config.ConsumerProperties;

@Component
public class EventStorageKafkaPublisher implements EventStoragePublisher {

    private static final Logger log = LoggerFactory.getLogger(EventStorageKafkaPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ConsumerProperties consumerProperties;

    public EventStorageKafkaPublisher(KafkaTemplate<String, Object> kafkaTemplate, ConsumerProperties consumerProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.consumerProperties = consumerProperties;
    }

    @Override
    public void publish(Event event) {
        try {
            kafkaTemplate.send(consumerProperties.getStorageTopic(), event.getDeviceId(), event).join();
            log.info("Dispatched event to storage topic. topic={}, eventId={}, deviceId={}",
                    consumerProperties.getStorageTopic(), event.getEventId(), event.getDeviceId());
        } catch (CompletionException exception) {
            throw new IllegalStateException("failed to dispatch event to storage topic", exception.getCause());
        }
    }
}
