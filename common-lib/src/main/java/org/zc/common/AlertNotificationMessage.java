package org.zc.common;

import lombok.Data;

@Data
public class AlertNotificationMessage {
    private Long alertRecordId;
    private Long ruleId;
    private String ruleName;
    private String deviceId;
    private String eventType;
    private String alertLevel;
    private String message;
    private long triggeredAt;
}
