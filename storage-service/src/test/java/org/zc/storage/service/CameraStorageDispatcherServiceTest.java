package org.zc.storage.service;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.zc.common.CameraMessage;
import org.zc.storage.config.CameraProperties;
import org.zc.storage.sentinel.CameraSentinelFacade;

class CameraStorageDispatcherServiceTest {

    private CameraDispatcherService dispatcherService;

    @AfterEach
    void tearDown() {
        if (dispatcherService != null) {
            dispatcherService.shutdown();
        }
    }

    @Test
    void shouldDispatchQueuedMessageToPersistenceWorker() {
        CameraProperties properties = buildProperties(2, 1, 1, 0L);
        CameraFailurePublisher failurePublisher = mock(CameraFailurePublisher.class);
        CameraPersistenceService persistenceService = mock(CameraPersistenceService.class);
        dispatcherService = new CameraDispatcherService(
                properties,
                failurePublisher,
                persistenceService,
                new CameraSentinelFacade());
        dispatcherService.startWorkers();
        CameraMessage cameraMessage = buildCamera("cam-1");

        dispatcherService.submit(cameraMessage);

        verify(persistenceService, timeout(1000)).persistBatch(anyList());
    }

    @Test
    void shouldDispatchBatchToPersistenceWorker() {
        CameraProperties properties = buildProperties(5, 1, 1, 0L);
        properties.setBatchSize(3);
        CameraFailurePublisher failurePublisher = mock(CameraFailurePublisher.class);
        CameraPersistenceService persistenceService = mock(CameraPersistenceService.class);
        dispatcherService = new CameraDispatcherService(
                properties,
                failurePublisher,
                persistenceService,
                new CameraSentinelFacade());

        dispatcherService.submit(buildCamera("cam-1"));
        dispatcherService.submit(buildCamera("cam-2"));
        dispatcherService.submit(buildCamera("cam-3"));
        dispatcherService.startWorkers();

        verify(persistenceService, timeout(1000)).persistBatch(eq(List.of(
                buildCamera("cam-1"),
                buildCamera("cam-2"),
                buildCamera("cam-3"))));
    }

    @Test
    void shouldPublishDlqWhenQueueRemainsFull() {
        CameraProperties properties = buildProperties(1, 2, 1, 0L);
        CameraFailurePublisher failurePublisher = mock(CameraFailurePublisher.class);
        CameraPersistenceService persistenceService = mock(CameraPersistenceService.class);
        dispatcherService = new CameraDispatcherService(
                properties,
                failurePublisher,
                persistenceService,
                new CameraSentinelFacade());
        CameraMessage first = buildCamera("cam-1");
        CameraMessage second = buildCamera("cam-2");

        dispatcherService.submit(first);
        dispatcherService.submit(second);

        verify(failurePublisher).publish(eq(second), eq("camera-enqueue"), eq("queue full"), eq(2));
    }

    private CameraProperties buildProperties(int queueCapacity, int retryTimes, int workerCount, long backoffMs) {
        CameraProperties properties = new CameraProperties();
        properties.setQueueCapacity(queueCapacity);
        properties.setEnqueueRetryTimes(retryTimes);
        properties.setWorkerCount(workerCount);
        properties.setEnqueueRetryBackoffMs(backoffMs);
        properties.setPollTimeoutMs(50L);
        return properties;
    }

    private CameraMessage buildCamera(String cameraId) {
        CameraMessage cameraMessage = new CameraMessage();
        cameraMessage.setCameraId(cameraId);
        cameraMessage.setStatus(1);
        return cameraMessage;
    }
}
