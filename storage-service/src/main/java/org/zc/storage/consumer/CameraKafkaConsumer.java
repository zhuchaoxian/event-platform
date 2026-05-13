package org.zc.storage.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.zc.common.CameraMessage;
import org.zc.storage.config.CameraProperties;
import org.zc.storage.service.CameraDispatcherService;

@Component
public class CameraKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(CameraKafkaConsumer.class);

    private final CameraDispatcherService cameraDispatcherService;
    private final CameraProperties cameraProperties;

    public CameraKafkaConsumer(
            CameraDispatcherService cameraDispatcherService,
            CameraProperties cameraProperties) {
        this.cameraDispatcherService = cameraDispatcherService;
        this.cameraProperties = cameraProperties;
    }

    @KafkaListener(
            topics = "${camera.topic:camera-topic}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "cameraKafkaListenerContainerFactory")
    public void onMessage(CameraMessage cameraMessage) {
        log.info("Received camera message from Kafka. topic={}, cameraId={}, status={}",
                cameraProperties.getTopic(),
                cameraMessage == null ? null : cameraMessage.getCameraId(),
                cameraMessage == null ? null : cameraMessage.getStatus());
        cameraDispatcherService.submit(cameraMessage);
    }
}
