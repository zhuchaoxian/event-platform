package org.zc.ingest.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.zc.common.CameraMessage;
import org.zc.ingest.producer.CameraKafkaProducer;

@Slf4j
@Service
@RequiredArgsConstructor
public class CameraIngestService {

    private final CameraKafkaProducer cameraKafkaProducer;

    public void ingest(CameraMessage cameraMessage, String topic) {
        normalize(cameraMessage);
        validate(cameraMessage);
        log.info(
                "received camera mqtt message. cameraId={}, topic={}, status={}",
                cameraMessage.getCameraId(),
                topic,
                cameraMessage.getStatus());
        cameraKafkaProducer.send(cameraMessage);
    }

    private void normalize(CameraMessage cameraMessage) {
        if (cameraMessage == null) {
            return;
        }
        if (cameraMessage.getCameraId() != null) {
            cameraMessage.setCameraId(cameraMessage.getCameraId().trim());
        }
        if (cameraMessage.getCameraName() != null) {
            cameraMessage.setCameraName(cameraMessage.getCameraName().trim());
        }
    }

    private void validate(CameraMessage cameraMessage) {
        if (cameraMessage == null) {
            throw new IllegalArgumentException("camera message must not be null");
        }
        if (!StringUtils.hasText(cameraMessage.getCameraId())) {
            throw new IllegalArgumentException("cameraId must not be blank");
        }
    }
}
