package org.zc.ingest.handler;

import java.nio.charset.StandardCharsets;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import org.zc.common.CameraMessage;
import org.zc.ingest.config.CameraMqttInboundConfiguration;
import org.zc.ingest.config.EventIngestProperties;
import org.zc.ingest.dto.MqttEventMessage;
import org.zc.ingest.parser.CameraMqttParser;
import org.zc.ingest.service.CameraIngestService;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "event.ingest.camera-mqtt", name = "enabled", havingValue = "true")
public class CameraMqttMessageHandler {

    private final CameraMqttParser cameraMqttParser;
    private final EventIngestProperties eventIngestProperties;
    private final CameraIngestService cameraIngestService;

    @ServiceActivator(inputChannel = CameraMqttInboundConfiguration.CAMERA_MQTT_INPUT_CHANNEL)
    public void handleMessage(Message<?> message) {
        String topic = headerAsString(message, MqttHeaders.RECEIVED_TOPIC);
        try {
            MqttEventMessage mqttEventMessage = new MqttEventMessage(
                    payloadAsString(message.getPayload()),
                    topic,
                    headerAsInteger(message, MqttHeaders.RECEIVED_QOS),
                    eventIngestProperties.getCameraMqtt().getClientId());
            CameraMessage cameraMessage = cameraMqttParser.parse(mqttEventMessage);
            cameraIngestService.ingest(cameraMessage, topic);
        } catch (Exception exception) {
            log.error("failed to process camera mqtt message. topic={}", topic, exception);
        }
    }

    private String payloadAsString(Object payload) {
        if (payload instanceof byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        return payload == null ? null : payload.toString();
    }

    private String headerAsString(Message<?> message, String key) {
        Object value = message.getHeaders().get(key);
        return value == null ? null : value.toString();
    }

    private Integer headerAsInteger(Message<?> message, String key) {
        Object value = message.getHeaders().get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        return Integer.parseInt(value.toString());
    }
}
