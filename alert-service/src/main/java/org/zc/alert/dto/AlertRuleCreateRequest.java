package org.zc.alert.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AlertRuleCreateRequest {
    @NotBlank
    private String ruleName;
    @NotBlank
    private String eventType;
    @NotBlank
    private String ruleType;
    @NotBlank
    private String threshold;
    private Integer cooldownS = 300;
    private String deviceId;
}
