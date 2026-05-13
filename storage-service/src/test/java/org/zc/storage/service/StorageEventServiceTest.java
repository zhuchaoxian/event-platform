package org.zc.storage.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.zc.common.Event;
import org.zc.storage.config.EventProperties;
import org.zc.storage.dao.EventDao;
import org.zc.storage.sentinel.EventSentinelFacade;
import org.zc.storage.sentinel.EventSentinelBlockedException;

class StorageEventServiceTest {

    @Test
    void shouldRetryAndSucceedOnThirdAttempt() {
        EventDao storageEventDao = mock(EventDao.class);
        FailurePublisher failurePublisher = mock(FailurePublisher.class);
        EventProperties properties = new EventProperties();
        properties.setRetryTimes(3);
        properties.setBatchSize(100);
        List<Event> events = List.of(buildEvent("evt-1"));

        when(storageEventDao.saveBatch(events))
                .thenThrow(new IllegalStateException("db-1"))
                .thenThrow(new IllegalStateException("db-2"))
                .thenReturn(1);

        EventService storageEventService =
                new EventService(
                        storageEventDao,
                        failurePublisher,
                        properties,
                        new EventSentinelFacade());
        storageEventService.persistBatch(events);

        verify(storageEventDao, times(3)).saveBatch(events);
        verify(failurePublisher, times(0)).publish(events.get(0), "db-storage", "db-2", 3);
    }

    @Test
    void shouldPublishFailureAfterThreeAttempts() {
        EventDao storageEventDao = mock(EventDao.class);
        FailurePublisher failurePublisher = mock(FailurePublisher.class);
        EventProperties properties = new EventProperties();
        properties.setRetryTimes(3);
        properties.setBatchSize(100);
        List<Event> events = List.of(buildEvent("evt-1"), buildEvent("evt-2"));

        when(storageEventDao.saveBatch(events)).thenThrow(new IllegalStateException("db down"));

        EventService storageEventService =
                new EventService(
                        storageEventDao,
                        failurePublisher,
                        properties,
                        new EventSentinelFacade());
        storageEventService.persistBatch(events);

        verify(storageEventDao, times(3)).saveBatch(events);
        verify(failurePublisher).publish(eq(events.get(0)), eq("db-storage"), eq("db down"), eq(3));
        verify(failurePublisher).publish(eq(events.get(1)), eq("db-storage"), eq("db down"), eq(3));
    }

    @Test
    void shouldPublishFailureWhenSentinelBlocksPersistence() {
        EventDao storageEventDao = mock(EventDao.class);
        FailurePublisher failurePublisher = mock(FailurePublisher.class);
        EventSentinelFacade sentinelFacade = mock(EventSentinelFacade.class);
        EventProperties properties = new EventProperties();
        properties.setRetryTimes(3);
        properties.setBatchSize(100);
        List<Event> events = List.of(buildEvent("evt-1"));

        doThrow(new EventSentinelBlockedException("sentinel persist blocked", new IllegalStateException("block")))
                .when(sentinelFacade)
                .persistBatch(storageEventDao, events);

        EventService storageEventService =
                new EventService(storageEventDao, failurePublisher, properties, sentinelFacade);
        storageEventService.persistBatch(events);

        verify(storageEventDao, times(0)).saveBatch(events);
        verify(failurePublisher).publish(eq(events.get(0)), eq("db-storage"), eq("sentinel persist blocked"), eq(3));
    }

    private Event buildEvent(String eventId) {
        Event event = new Event();
        event.setEventId(eventId);
        event.setDeviceId("device-1");
        event.setType("temperature");
        event.setTimestamp(123L);
        return event;
    }
}
