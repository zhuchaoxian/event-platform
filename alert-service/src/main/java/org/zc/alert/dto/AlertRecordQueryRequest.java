package org.zc.alert.dto;

import lombok.Data;

@Data
public class AlertRecordQueryRequest {
    private int page = 1;
    private int size = 10;
    private String status;
    private String eventType;
    private String deviceId;
}
