package org.zc.storage.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.zc.common.Event;
import org.zc.storage.entity.EventRecord;
import org.zc.storage.mapper.EventMapper;

class StorageEventDaoImplTest {

    @Test
    void shouldConvertEventAndInsertBatch() {
        EventMapper mapper = mock(EventMapper.class);
        when(mapper.insertBatch(anyList())).thenReturn(1);
        EventDaoImpl dao = new EventDaoImpl(mapper, new ObjectMapper());

        Event event = new Event();
        event.setEventId("evt-1");
        event.setDeviceId("device-1");
        event.setTimestamp(123L);
        event.setType("temperature");
        event.setPayload(Map.of("value", 21));

        int affected = dao.saveBatch(List.of(event));

        assertEquals(1, affected);
        ArgumentCaptor<List<EventRecord>> captor = ArgumentCaptor.forClass(List.class);
        verify(mapper).insertBatch(captor.capture());
        EventRecord record = captor.getValue().get(0);
        assertEquals("evt-1", record.getEventId());
        assertEquals("device-1", record.getDeviceId());
        assertEquals(123L, record.getEventTimestamp());
        assertEquals("temperature", record.getEventType());
        assertEquals("{\"value\":21}", record.getPayloadJson());
        assertNotNull(record.getCreatedAt());
    }
}
