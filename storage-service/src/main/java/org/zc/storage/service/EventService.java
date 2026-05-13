package org.zc.storage.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.zc.common.Event;
import org.zc.storage.config.EventProperties;
import org.zc.storage.dao.EventDao;
import org.zc.storage.sentinel.EventSentinelFacade;
import org.zc.storage.sentinel.EventSentinelBlockedException;

@Service
public class EventService {

    private static final Logger log = LoggerFactory.getLogger(EventService.class);

    private final EventDao eventDao;
    private final FailurePublisher failurePublisher;
    private final EventProperties eventProperties;
    private final EventSentinelFacade eventSentinelFacade;

    public EventService(
            EventDao eventDao,
            FailurePublisher failurePublisher,
            EventProperties eventProperties,
            EventSentinelFacade eventSentinelFacade) {
        this.eventDao = eventDao;
        this.failurePublisher = failurePublisher;
        this.eventProperties = eventProperties;
        this.eventSentinelFacade = eventSentinelFacade;
    }

    public void persistBatch(List<Event> events) {
        List<Event> validEvents = sanitize(events);
        if (validEvents.isEmpty()) {
            return;
        }

        int batchSize = Math.max(1, eventProperties.getBatchSize());
        for (int start = 0; start < validEvents.size(); start += batchSize) {
            int end = Math.min(start + batchSize, validEvents.size());
            persistChunkWithRetry(validEvents.subList(start, end));
        }
    }

    private void persistChunkWithRetry(List<Event> events) {
        int retryTimes = Math.max(1, eventProperties.getRetryTimes());
        Exception lastException = null;

        for (int attempt = 1; attempt <= retryTimes; attempt++) {
            try {
                int affected = eventSentinelFacade.persistBatch(eventDao, events);
                log.info("Persisted events to MySQL. size={}, affectedRows={}, attempt={}",
                        events.size(), affected, attempt);
                return;
            } catch (EventSentinelBlockedException exception) {
                lastException = exception;
                log.warn("Event batch persistence blocked by Sentinel. size={}, attempt={}/{}",
                        events.size(), attempt, retryTimes, exception);
            } catch (Exception exception) {
                lastException = exception;
                log.warn("Failed to persist event batch. size={}, attempt={}/{}",
                        events.size(), attempt, retryTimes, exception);
            }
        }

        String reason = buildReason(lastException);
        for (Event event : events) {
            failurePublisher.publish(event, "db-storage", reason, retryTimes);
        }
    }

    private String buildReason(Exception exception) {
        if (exception == null) {
            return "unknown storage failure";
        }
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    private List<Event> sanitize(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return Collections.emptyList();
        }

        List<Event> validEvents = new ArrayList<>(events.size());
        for (Event event : events) {
            if (event != null) {
                validEvents.add(event);
            }
        }
        return validEvents;
    }
}
