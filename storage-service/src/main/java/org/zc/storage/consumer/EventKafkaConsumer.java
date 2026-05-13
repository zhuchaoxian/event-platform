package org.zc.storage.consumer;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.zc.common.Event;
import org.zc.storage.config.EventProperties;
import org.zc.storage.service.EventService;

@Component
public class EventKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(EventKafkaConsumer.class);

    private final EventService eventService;
    private final EventProperties eventProperties;

    public EventKafkaConsumer(EventService eventService, EventProperties eventProperties) {
        this.eventService = eventService;
        this.eventProperties = eventProperties;
    }

    @KafkaListener(
            topics = "${event.topic:event-storage-topic}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "eventKafkaListenerContainerFactory")
    public void onMessage(List<Event> events) {
        int size = events == null ? 0 : events.size();
        log.info("Received storage batch from Kafka. topic={}, batchSize={}",
                eventProperties.getTopic(), size);
        eventService.persistBatch(events);
    }
}
