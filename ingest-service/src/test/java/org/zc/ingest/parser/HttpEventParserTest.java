package org.zc.ingest.parser;

import org.junit.jupiter.api.Test;
import org.zc.common.Event;
import org.zc.ingest.dto.EventReportRequest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HttpEventParserTest {

    @Test
    void shouldAttachHttpSourceMetadata() {
        HttpEventParser parser = new HttpEventParser(new EventPayloadMetadataHelper());
        EventReportRequest request = new EventReportRequest();
        request.setEventId("evt-1");
        request.setDeviceId("device-1");
        request.setTimestamp(1000L);
        request.setType("temperature");
        request.setPayload(Map.of("value", 21));

        Event event = parser.parse(request);

        assertEquals("evt-1", event.getEventId());
        assertEquals("device-1", event.getDeviceId());
        assertEquals(1000L, event.getTimestamp());
        assertEquals("temperature", event.getType());
        assertEquals("http", ((Map<?, ?>) event.getPayload().get("_meta")).get("source"));
    }
}
