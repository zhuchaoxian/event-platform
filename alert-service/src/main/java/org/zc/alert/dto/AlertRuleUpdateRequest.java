package org.zc.alert.dto;

import lombok.Data;

@Data
public class AlertRuleUpdateRequest {
    private String ruleName;
    private String eventType;
    private String ruleType;
    private String threshold;
    private Integer cooldownS;
    private String deviceId;
}
