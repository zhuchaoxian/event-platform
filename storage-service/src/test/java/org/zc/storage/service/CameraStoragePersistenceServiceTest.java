package org.zc.storage.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.zc.common.CameraMessage;
import org.zc.storage.config.CameraProperties;
import org.zc.storage.dao.CameraDao;
import org.zc.storage.sentinel.CameraSentinelFacade;

class CameraStoragePersistenceServiceTest {

    @Test
    void shouldRetryAndSucceedOnThirdAttempt() {
        CameraDao cameraStorageDao = mock(CameraDao.class);
        CameraFailurePublisher failurePublisher = mock(CameraFailurePublisher.class);
        CameraProperties properties = new CameraProperties();
        properties.setDbRetryTimes(3);
        CameraMessage cameraMessage = buildCamera();
        List<CameraMessage> cameraMessages = List.of(cameraMessage);

        when(cameraStorageDao.saveBatch(cameraMessages))
                .thenThrow(new IllegalStateException("db-1"))
                .thenThrow(new IllegalStateException("db-2"))
                .thenReturn(1);

        CameraPersistenceService persistenceService = new CameraPersistenceService(
                cameraStorageDao,
                failurePublisher,
                properties,
                new CameraSentinelFacade());
        persistenceService.persistBatch(cameraMessages);

        verify(cameraStorageDao, times(3)).saveBatch(cameraMessages);
        verify(failurePublisher, times(0)).publish(eq(cameraMessage), eq("camera-db-storage"), eq("db-2"), eq(3));
    }

    @Test
    void shouldPublishFailureAfterRetryExhausted() {
        CameraDao cameraStorageDao = mock(CameraDao.class);
        CameraFailurePublisher failurePublisher = mock(CameraFailurePublisher.class);
        CameraProperties properties = new CameraProperties();
        properties.setDbRetryTimes(3);
        CameraMessage cameraMessage = buildCamera();
        List<CameraMessage> cameraMessages = List.of(cameraMessage);

        when(cameraStorageDao.saveBatch(cameraMessages)).thenThrow(new IllegalStateException("db down"));

        CameraPersistenceService persistenceService = new CameraPersistenceService(
                cameraStorageDao,
                failurePublisher,
                properties,
                new CameraSentinelFacade());
        persistenceService.persistBatch(cameraMessages);

        verify(cameraStorageDao, times(3)).saveBatch(cameraMessages);
        verify(failurePublisher).publish(eq(cameraMessage), eq("camera-db-storage"), eq("db down"), eq(3));
    }

    private CameraMessage buildCamera() {
        CameraMessage cameraMessage = new CameraMessage();
        cameraMessage.setCameraId("cam-1");
        cameraMessage.setStatus(1);
        return cameraMessage;
    }
}
