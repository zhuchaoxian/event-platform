package org.zc.storage.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.zc.common.CameraMessage;
import org.zc.storage.config.CameraProperties;
import org.zc.storage.dao.CameraDao;
import org.zc.storage.sentinel.CameraSentinelBlockedException;
import org.zc.storage.sentinel.CameraSentinelFacade;

@Service
public class CameraPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(CameraPersistenceService.class);

    private final CameraDao cameraDao;
    private final CameraFailurePublisher cameraFailurePublisher;
    private final CameraProperties cameraProperties;
    private final CameraSentinelFacade sentinelFacade;

    public CameraPersistenceService(
            CameraDao cameraDao,
            CameraFailurePublisher cameraFailurePublisher,
            CameraProperties cameraProperties,
            CameraSentinelFacade sentinelFacade) {
        this.cameraDao = cameraDao;
        this.cameraFailurePublisher = cameraFailurePublisher;
        this.cameraProperties = cameraProperties;
        this.sentinelFacade = sentinelFacade;
    }

    public void persistBatch(List<CameraMessage> cameraMessages) {
        List<CameraMessage> validMessages = sanitize(cameraMessages);
        if (validMessages.isEmpty()) {
            return;
        }

        int retryTimes = Math.max(1, cameraProperties.getDbRetryTimes());
        String reason = "unknown storage failure";

        for (int attempt = 1; attempt <= retryTimes; attempt++) {
            try {
                int affected = sentinelFacade.persistBatch(cameraDao, validMessages);
                log.info("Persisted camera batch to MySQL. batchSize={}, affectedRows={}, attempt={}",
                        validMessages.size(),
                        affected,
                        attempt);
                return;
            } catch (CameraSentinelBlockedException exception) {
                reason = exception.getMessage();
                log.warn("Camera batch persistence blocked by Sentinel. batchSize={}, attempt={}/{}",
                        validMessages.size(),
                        attempt,
                        retryTimes,
                        exception);
            } catch (Exception exception) {
                reason = buildReason(exception);
                log.warn("Failed to persist camera batch. batchSize={}, attempt={}/{}",
                        validMessages.size(),
                        attempt,
                        retryTimes,
                        exception);
            }
        }

        for (CameraMessage cameraMessage : validMessages) {
            cameraFailurePublisher.publish(cameraMessage, "camera-db-storage", reason, retryTimes);
        }
    }

    private String buildReason(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    private List<CameraMessage> sanitize(List<CameraMessage> cameraMessages) {
        if (cameraMessages == null || cameraMessages.isEmpty()) {
            return Collections.emptyList();
        }

        List<CameraMessage> validMessages = new ArrayList<>(cameraMessages.size());
        for (CameraMessage cameraMessage : cameraMessages) {
            if (cameraMessage != null) {
                validMessages.add(cameraMessage);
            }
        }
        return validMessages;
    }
}
