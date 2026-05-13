package org.zc.ingest.parser;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.zc.common.Event;
import org.zc.ingest.dto.EventReportRequest;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class HttpEventParser {
    private final EventPayloadMetadataHelper metadataHelper;

    public Event parse(EventReportRequest request) {
        Event event = new Event();
        event.setEventId(request.getEventId());
        event.setDeviceId(request.getDeviceId());
        event.setTimestamp(request.getTimestamp());
        event.setType(request.getType());
        event.setPayload(metadataHelper.attachMetadata(request.getPayload(), Map.of("source", "http")));
        return event;
    }
}
