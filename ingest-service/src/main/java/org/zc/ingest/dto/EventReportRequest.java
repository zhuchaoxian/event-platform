package org.zc.ingest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class EventReportRequest {
    private String eventId;

    @NotBlank(message = "deviceId must not be blank")
    private String deviceId;

    @NotNull(message = "timestamp must not be null")
    private Long timestamp;

    private String type;
    private Map<String, Object> payload;
}
