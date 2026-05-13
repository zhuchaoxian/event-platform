package org.zc.storage.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.zc.common.CameraFailureMessage;
import org.zc.common.CameraMessage;
import org.zc.storage.config.CameraProperties;

@Component
public class CameraFailureKafkaPublisher implements CameraFailurePublisher {

    private static final Logger log = LoggerFactory.getLogger(CameraFailureKafkaPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final CameraProperties cameraProperties;

    public CameraFailureKafkaPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            CameraProperties cameraProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.cameraProperties = cameraProperties;
    }

    @Override
    public void publish(CameraMessage cameraMessage, String stage, String reason, int attempts) {
        CameraFailureMessage message = new CameraFailureMessage();
        message.setCamera(cameraMessage);
        message.setStage(stage);
        message.setReason(reason);
        message.setAttempts(attempts);
        message.setFailedAt(System.currentTimeMillis());

        String messageKey = cameraMessage == null ? "unknown-camera" : cameraMessage.getCameraId();
        kafkaTemplate.send(cameraProperties.getFailureTopic(), messageKey, message);
        log.warn("Published camera storage failure message. topic={}, cameraId={}, stage={}, attempts={}",
                cameraProperties.getFailureTopic(),
                cameraMessage == null ? null : cameraMessage.getCameraId(),
                stage,
                attempts);
    }
}
