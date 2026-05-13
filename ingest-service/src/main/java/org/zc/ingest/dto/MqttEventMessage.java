package org.zc.ingest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MqttEventMessage {
    private String payload;
    private String topic;
    private Integer qos;
    private String clientId;
}
