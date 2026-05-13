package org.zc.ingest.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.zc.common.CameraMessage;
import org.zc.ingest.dto.MqttEventMessage;

@Component
@RequiredArgsConstructor
public class CameraMqttParser {

    private final ObjectMapper objectMapper;

    public CameraMessage parse(MqttEventMessage message) {
        if (!StringUtils.hasText(message.getPayload())) {
            throw new IllegalArgumentException("camera mqtt payload must not be blank");
        }

        try {
            return objectMapper.readValue(message.getPayload(), CameraMessage.class);
        } catch (Exception exception) {
            throw new IllegalArgumentException("camera mqtt payload must be valid JSON", exception);
        }
    }
}
