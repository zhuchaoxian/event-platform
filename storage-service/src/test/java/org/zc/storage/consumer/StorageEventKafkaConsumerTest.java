package org.zc.storage.consumer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.zc.common.Event;
import org.zc.storage.config.EventProperties;
import org.zc.storage.service.EventService;

class StorageEventKafkaConsumerTest {

    @Test
    void shouldDelegateBatchToStorageService() {
        EventService storageEventService = mock(EventService.class);
        EventProperties properties = new EventProperties();
        EventKafkaConsumer consumer = new EventKafkaConsumer(storageEventService, properties);
        List<Event> events = List.of(buildEvent());

        consumer.onMessage(events);

        verify(storageEventService).persistBatch(events);
    }

    private Event buildEvent() {
        Event event = new Event();
        event.setEventId("evt-1");
        event.setDeviceId("device-1");
        event.setType("temperature");
        event.setTimestamp(123L);
        return event;
    }
}
