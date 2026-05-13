package org.zc.alert.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.zc.alert.config.AlertProperties;
import org.zc.alert.entity.AlertRecord;
import org.zc.common.AlertNotificationMessage;

import java.time.ZoneId;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlertNotificationProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final AlertProperties alertProperties;

    public void send(AlertRecord record) {
        AlertNotificationMessage message = new AlertNotificationMessage();
        message.setAlertRecordId(record.getId());
        message.setRuleId(record.getRuleId());
        message.setRuleName(record.getRuleName());
        message.setDeviceId(record.getDeviceId());
        message.setEventType(record.getEventType());
        message.setAlertLevel(record.getAlertLevel());
        message.setMessage(record.getMessage());
        message.setTriggeredAt(record.getTriggeredAt() != null
                ? record.getTriggeredAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                : System.currentTimeMillis());

        String topic = alertProperties.getKafka().getNotificationTopic();
        kafkaTemplate.send(topic, record.getDeviceId(), message).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to send alert notification. alertId={}, topic={}",
                        record.getId(), topic, ex);
            } else {
                log.info("Alert notification sent. alertId={}, topic={}, offset={}",
                        record.getId(), topic,
                        result != null ? result.getRecordMetadata().offset() : -1);
            }
        });
    }
}
