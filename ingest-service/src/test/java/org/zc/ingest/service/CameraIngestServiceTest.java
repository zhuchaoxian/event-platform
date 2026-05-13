package org.zc.ingest.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.zc.common.CameraMessage;
import org.zc.ingest.producer.CameraKafkaProducer;

class CameraIngestServiceTest {

    @Test
    void shouldTrimAndSendCameraMessage() {
        CameraKafkaProducer producer = mock(CameraKafkaProducer.class);
        CameraIngestService service = new CameraIngestService(producer);
        CameraMessage cameraMessage = new CameraMessage();
        cameraMessage.setCameraId(" cam-1 ");
        cameraMessage.setCameraName(" front gate ");
        cameraMessage.setStatus(1);

        service.ingest(cameraMessage, "Camera/cam-1");

        ArgumentCaptor<CameraMessage> captor = ArgumentCaptor.forClass(CameraMessage.class);
        verify(producer, times(1)).send(captor.capture());
        CameraMessage sentMessage = captor.getValue();
        assertEquals("cam-1", sentMessage.getCameraId());
        assertEquals("front gate", sentMessage.getCameraName());
    }

    @Test
    void shouldRejectMissingCameraId() {
        CameraKafkaProducer producer = mock(CameraKafkaProducer.class);
        CameraIngestService service = new CameraIngestService(producer);
        CameraMessage cameraMessage = new CameraMessage();

        assertThrows(IllegalArgumentException.class, () -> service.ingest(cameraMessage, "Camera/cam-1"));
    }
}
