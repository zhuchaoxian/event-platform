package org.zc.ingest.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.zc.common.Event;
import org.zc.ingest.dto.MqttEventMessage;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MqttEventParserTest {

    @Test
    void shouldParseMqttJsonAndAttachMetadata() {
        MqttEventParser parser = new MqttEventParser(new ObjectMapper(), new EventPayloadMetadataHelper());
        MqttEventMessage message = new MqttEventMessage(
            "{\"eventId\":\"evt-1\",\"deviceId\":\"device-1\",\"timestamp\":123,\"type\":\"temperature\",\"payload\":{\"value\":18}}",
            "event/device/device-1",
            1,
            "mqtt-client"
        );

        Event event = parser.parse(message);

        assertEquals("evt-1", event.getEventId());
        assertEquals("device-1", event.getDeviceId());
        assertEquals(123L, event.getTimestamp());
        assertEquals("temperature", event.getType());
        Map<?, ?> metadata = (Map<?, ?>) event.getPayload().get("_meta");
        assertEquals("mqtt", metadata.get("source"));
        assertEquals("event/device/device-1", metadata.get("topic"));
        assertEquals(1, metadata.get("qos"));
        assertEquals("mqtt-client", metadata.get("clientId"));
    }

    @Test
    void shouldRejectInvalidJsonPayload() {
        MqttEventParser parser = new MqttEventParser(new ObjectMapper(), new EventPayloadMetadataHelper());
        MqttEventMessage message = new MqttEventMessage("not-json", "event/#", 1, "mqtt-client");

        assertThrows(IllegalArgumentException.class, () -> parser.parse(message));
    }
}
