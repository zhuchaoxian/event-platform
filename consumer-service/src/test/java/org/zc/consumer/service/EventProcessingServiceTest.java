package org.zc.consumer.service;

import org.junit.jupiter.api.Test;
import org.zc.common.Event;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class EventProcessingServiceTest {

    @Test
    void shouldDispatchToStorageTopic() {
        EventStoragePublisher storagePublisher = mock(EventStoragePublisher.class);
        EventProcessingService processingService = new EventProcessingService(storagePublisher);

        Event event = buildEvent();
        processingService.process(event);

        verify(storagePublisher, times(1)).publish(event);
    }

    @Test
    void shouldRejectBlankDeviceId() {
        EventStoragePublisher storagePublisher = mock(EventStoragePublisher.class);
        EventProcessingService processingService = new EventProcessingService(storagePublisher);
        Event event = buildEvent();
        event.setDeviceId(" ");

        assertThrows(IllegalArgumentException.class, () -> processingService.process(event));
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
