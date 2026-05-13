package org.zc.ingest.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.zc.common.CameraMessage;
import org.zc.ingest.dto.MqttEventMessage;

class CameraMqttParserTest {

    @Test
    void shouldParseCameraPayload() {
        CameraMqttParser parser = new CameraMqttParser(new ObjectMapper());
        MqttEventMessage message = new MqttEventMessage(
                "{\"cameraId\":\"cam-1\",\"cameraName\":\"north gate\",\"latitude\":30.1,\"longitude\":120.2,\"status\":1}",
                "Camera/cam-1",
                1,
                "camera-mqtt-client");

        CameraMessage cameraMessage = parser.parse(message);

        assertEquals("cam-1", cameraMessage.getCameraId());
        assertEquals("north gate", cameraMessage.getCameraName());
        assertEquals(30.1d, cameraMessage.getLatitude());
        assertEquals(120.2d, cameraMessage.getLongitude());
        assertEquals(1, cameraMessage.getStatus());
    }

    @Test
    void shouldRejectInvalidCameraPayload() {
        CameraMqttParser parser = new CameraMqttParser(new ObjectMapper());
        MqttEventMessage message = new MqttEventMessage("not-json", "Camera/#", 1, "camera-mqtt-client");

        assertThrows(IllegalArgumentException.class, () -> parser.parse(message));
    }
}
