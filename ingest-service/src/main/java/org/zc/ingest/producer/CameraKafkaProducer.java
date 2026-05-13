package org.zc.ingest.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.zc.common.CameraMessage;
import org.zc.ingest.config.EventIngestProperties;

@Slf4j
@Component
@RequiredArgsConstructor
public class CameraKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final EventIngestProperties eventIngestProperties;

    public void send(CameraMessage cameraMessage) {
        kafkaTemplate.send(eventIngestProperties.getCameraTopic(), cameraMessage.getCameraId(), cameraMessage);
        log.info(
                "camera message published to kafka. topic={}, cameraId={}, status={}",
                eventIngestProperties.getCameraTopic(),
                cameraMessage.getCameraId(),
                cameraMessage.getStatus());
    }
}
