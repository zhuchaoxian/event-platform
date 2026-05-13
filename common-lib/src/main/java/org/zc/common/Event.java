package org.zc.common;

import lombok.Data;

import java.util.Map;

@Data
public class Event {
    private String eventId;
    private String deviceId;
    private Long timestamp;
    private String type;
    private Map<String, Object> payload;
}
