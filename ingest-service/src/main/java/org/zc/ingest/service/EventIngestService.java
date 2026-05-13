package org.zc.ingest.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.zc.common.Event;
import org.zc.ingest.producer.EventKafkaProducer;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventIngestService {
    private final EventKafkaProducer eventKafkaProducer;

    public void ingest(Event event, String traceId, String source) {
        normalize(event);
        validate(event);
        event.setEventId(resolveEventId(event));

        String effectiveTraceId = StringUtils.hasText(traceId) ? traceId : event.getEventId();
        try (MDC.MDCCloseable ignored = MDC.putCloseable("traceId", effectiveTraceId)) {
            log.info(
                "received {} event. eventId={}, deviceId={}, timestamp={}, type={}",
                source,
                event.getEventId(),
                event.getDeviceId(),
                event.getTimestamp(),
                event.getType()
            );
            eventKafkaProducer.send(event);
        }
    }

    private void normalize(Event event) {
        if (event == null) {
            return;
        }
        if (event.getDeviceId() != null) {
            event.setDeviceId(event.getDeviceId().trim());
        }
        if (event.getType() != null) {
            event.setType(event.getType().trim());
        }
    }

    private void validate(Event event) {
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }
        if (!StringUtils.hasText(event.getDeviceId())) {
            throw new IllegalArgumentException("deviceId must not be blank");
        }
        if (event.getTimestamp() == null) {
            throw new IllegalArgumentException("timestamp must not be null");
        }
        if (event.getTimestamp() <= 0) {
            throw new IllegalArgumentException("timestamp must be greater than 0");
        }
    }

    private String resolveEventId(Event event) {
        if (StringUtils.hasText(event.getEventId())) {
            return event.getEventId().trim();
        }
        return UUID.randomUUID().toString().replace("-", "");
    }
}
