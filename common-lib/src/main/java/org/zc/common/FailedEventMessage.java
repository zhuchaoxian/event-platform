package org.zc.common;

import lombok.Data;

@Data
public class FailedEventMessage {
    private Event event;
    private String stage;
    private String reason;
    private int attempts;
    private long failedAt;
}
