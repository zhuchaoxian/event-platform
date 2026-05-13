package org.zc.ingest.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.zc.common.Event;
import org.zc.ingest.producer.EventKafkaProducer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class EventIngestServiceTest {

    @Test
    void shouldGenerateEventIdAndSendToKafka() {
        EventKafkaProducer producer = mock(EventKafkaProducer.class);
        EventIngestService service = new EventIngestService(producer);
        Event event = new Event();
        event.setDeviceId(" device-1 ");
        event.setTimestamp(1000L);
        event.setType(" temperature ");

        service.ingest(event, null, "http");

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(producer, times(1)).send(captor.capture());
        Event sentEvent = captor.getValue();
        assertNotNull(sentEvent.getEventId());
        assertEquals("device-1", sentEvent.getDeviceId());
        assertEquals("temperature", sentEvent.getType());
    }

    @Test
    void shouldRejectMissingDeviceId() {
        EventKafkaProducer producer = mock(EventKafkaProducer.class);
        EventIngestService service = new EventIngestService(producer);
        Event event = new Event();
        event.setTimestamp(1000L);

        assertThrows(IllegalArgumentException.class, () -> service.ingest(event, null, "mqtt"));
    }
}
