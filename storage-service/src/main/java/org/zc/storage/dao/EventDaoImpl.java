package org.zc.storage.dao;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Repository;
import org.zc.common.Event;
import org.zc.storage.entity.EventRecord;
import org.zc.storage.mapper.EventMapper;

@Repository
public class EventDaoImpl implements EventDao {

    private final EventMapper eventMapper;
    private final ObjectMapper objectMapper;

    public EventDaoImpl(EventMapper eventMapper, ObjectMapper objectMapper) {
        this.eventMapper = eventMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public int saveBatch(List<Event> events) {
        List<EventRecord> records = new ArrayList<>(events.size());
        for (Event event : events) {
            records.add(toRecord(event));
        }
        return eventMapper.insertBatch(records);
    }

    private EventRecord toRecord(Event event) {
        EventRecord record = new EventRecord();
        record.setEventId(event.getEventId());
        record.setDeviceId(event.getDeviceId());
        record.setEventTimestamp(event.getTimestamp());
        record.setEventType(event.getType());
        record.setPayloadJson(serializePayload(event));
        record.setCreatedAt(LocalDateTime.now());
        return record;
    }

    private String serializePayload(Event event) {
        if (event.getPayload() == null || event.getPayload().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(event.getPayload());
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("failed to serialize event payload", exception);
        }
    }
}
