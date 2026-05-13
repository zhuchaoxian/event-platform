package org.zc.alert.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.zc.alert.service.AlertEventService;
import org.zc.common.Event;

import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventAlertConsumer {

    private final AlertEventService alertEventService;
    private final Executor alertEventExecutor;

    @KafkaListener(
            topics = "${alert.kafka.topic:event-topic}",
            groupId = "${alert.kafka.consumer-group:alert-service-group}",
            containerFactory = "alertKafkaListenerContainerFactory")
    public void onMessage(Event event) {
        String traceId = event.getEventId() != null ? event.getEventId() : UUID.randomUUID().toString();
        try (MDC.MDCCloseable ignored = MDC.putCloseable("traceId", traceId)) {
            log.info("Alert consumer received event. eventId={}, deviceId={}, type={}",
                    event.getEventId(), event.getDeviceId(), event.getType());
            alertEventExecutor.execute(() -> {
                try {
                    alertEventService.process(event);
                } catch (Exception e) {
                    log.error("Failed to process alert for event. eventId={}", event.getEventId(), e);
                }
            });
        } catch (RejectedExecutionException e) {
            log.error("Alert executor saturated. eventId={}", event.getEventId(), e);
        }
    }
}
