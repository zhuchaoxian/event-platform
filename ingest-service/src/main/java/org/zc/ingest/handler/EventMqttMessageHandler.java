package org.zc.ingest.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import org.zc.common.Event;
import org.zc.ingest.config.EventIngestProperties;
import org.zc.ingest.config.EventMqttInboundConfiguration;
import org.zc.ingest.dto.MqttEventMessage;
import org.zc.ingest.parser.MqttEventParser;
import org.zc.ingest.service.EventIngestService;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventMqttMessageHandler {
    private final MqttEventParser mqttEventParser;
    private final EventIngestProperties eventIngestProperties;
    private final EventIngestService eventIngestService;

    @ServiceActivator(inputChannel = EventMqttInboundConfiguration.MQTT_INPUT_CHANNEL)
    public void handleMessage(Message<?> message) {
        String topic = headerAsString(message, MqttHeaders.RECEIVED_TOPIC);
        try {
            MqttEventMessage mqttEventMessage = new MqttEventMessage(
                payloadAsString(message.getPayload()),
                topic,
                headerAsInteger(message, MqttHeaders.RECEIVED_QOS),
                eventIngestProperties.getEventMqtt().getClientId()
            );
            Event event = mqttEventParser.parse(mqttEventMessage);
            eventIngestService.ingest(event, null, "mqtt");
        } catch (Exception exception) {
            log.error("failed to process mqtt message. topic={}", topic, exception);
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
