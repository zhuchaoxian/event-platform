package org.zc.storage.service;

import org.zc.common.CameraMessage;

public interface CameraFailurePublisher {

    void publish(CameraMessage cameraMessage, String stage, String reason, int attempts);
}
