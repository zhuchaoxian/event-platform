package org.zc.consumer.consumer;

import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.task.TaskExecutor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.zc.common.Event;
import org.zc.consumer.service.EventFailurePublisher;
import org.zc.consumer.service.EventProcessingService;

@Component
public class EventKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(EventKafkaConsumer.class);

    private final TaskExecutor eventConsumerExecutor;
    private final EventProcessingService eventProcessingService;
    private final EventFailurePublisher eventFailurePublisher;

    public EventKafkaConsumer(
        TaskExecutor eventConsumerExecutor,
        EventProcessingService eventProcessingService,
        EventFailurePublisher eventFailurePublisher
    ) {
        this.eventConsumerExecutor = eventConsumerExecutor;
        this.eventProcessingService = eventProcessingService;
        this.eventFailurePublisher = eventFailurePublisher;
    }

    @KafkaListener(
            topics = "${event.consumer.topic:event-topic}",
            groupId = "${spring.kafka.consumer.group-id}")
    public void onMessage(Event event, @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        String traceId = resolveTraceId(event);
        try (MDC.MDCCloseable ignored = MDC.putCloseable("traceId", traceId)) {
            log.info("Received event from Kafka. topic={}, key={}, eventId={}, deviceId={}",
                    "event-topic", key, event.getEventId(), event.getDeviceId());
            eventConsumerExecutor.execute(() -> processAsync(event));
        } catch (RejectedExecutionException ex) {
            log.error("Consumer executor is saturated. eventId={}, deviceId={}",
                    event.getEventId(), event.getDeviceId(), ex);
            eventFailurePublisher.publish(event, "executor", ex.getMessage(), 0);
        }
    }

    private void processAsync(Event event) {
        try {
            eventProcessingService.process(event);
        } catch (Exception ex) {
            log.error("Failed to process event asynchronously. eventId={}, deviceId={}",
                    event.getEventId(), event.getDeviceId(), ex);
            eventFailurePublisher.publish(event, "async-process", ex.getMessage(), 0);
        }
    }

    private String resolveTraceId(Event event) {
        if (event != null && event.getEventId() != null && !event.getEventId().isBlank()) {
            return event.getEventId();
        }
        return UUID.randomUUID().toString();
    }
}
