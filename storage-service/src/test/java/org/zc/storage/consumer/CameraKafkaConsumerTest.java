package org.zc.storage.consumer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.zc.common.CameraMessage;
import org.zc.storage.config.CameraProperties;
import org.zc.storage.service.CameraDispatcherService;

class CameraKafkaConsumerTest {

    @Test
    void shouldDelegateMessageToDispatcher() {
        CameraDispatcherService dispatcherService = mock(CameraDispatcherService.class);
        CameraProperties properties = new CameraProperties();
        CameraKafkaConsumer consumer = new CameraKafkaConsumer(dispatcherService, properties);
        CameraMessage cameraMessage = new CameraMessage();
        cameraMessage.setCameraId("cam-1");

        consumer.onMessage(cameraMessage);

        verify(dispatcherService).submit(cameraMessage);
    }
}
