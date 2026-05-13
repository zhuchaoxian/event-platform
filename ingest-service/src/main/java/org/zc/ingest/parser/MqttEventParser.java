package org.zc.ingest.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.zc.common.Event;
import org.zc.ingest.dto.EventReportRequest;
import org.zc.ingest.dto.MqttEventMessage;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MqttEventParser {
    private final ObjectMapper objectMapper;
    private final EventPayloadMetadataHelper metadataHelper;

    public Event parse(MqttEventMessage message) {
        if (!StringUtils.hasText(message.getPayload())) {
            throw new IllegalArgumentException("mqtt payload must not be blank");
        }

        EventReportRequest request;
        try {
            request = objectMapper.readValue(message.getPayload(), EventReportRequest.class);
        } catch (Exception exception) {
            throw new IllegalArgumentException("mqtt payload must be valid JSON", exception);
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("source", "mqtt");
        metadata.put("topic", message.getTopic());
        metadata.put("qos", message.getQos());
        metadata.put("clientId", message.getClientId());
        metadata.put("receivedAt", System.currentTimeMillis());

        Event event = new Event();
        event.setEventId(request.getEventId());
        event.setDeviceId(request.getDeviceId());
        event.setTimestamp(request.getTimestamp());
        event.setType(request.getType());
        event.setPayload(metadataHelper.attachMetadata(request.getPayload(), metadata));
        return event;
    }
}
