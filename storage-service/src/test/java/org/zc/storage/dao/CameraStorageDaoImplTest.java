package org.zc.storage.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.zc.common.CameraMessage;
import org.zc.storage.entity.CameraRecord;
import org.zc.storage.mapper.CameraMapper;

class CameraStorageDaoImplTest {

    @Test
    void shouldConvertCameraMessageAndBatchUpsert() {
        CameraMapper mapper = mock(CameraMapper.class);
        when(mapper.upsertBatch(any())).thenReturn(1);
        CameraDaoImpl dao = new CameraDaoImpl(mapper);

        CameraMessage cameraMessage = new CameraMessage();
        cameraMessage.setCameraId("cam-1");
        cameraMessage.setCameraName("north gate");
        cameraMessage.setLatitude(30.1d);
        cameraMessage.setLongitude(120.2d);
        cameraMessage.setStatus(1);

        int affected = dao.saveBatch(List.of(cameraMessage));

        assertEquals(1, affected);
        ArgumentCaptor<List<CameraRecord>> captor = ArgumentCaptor.forClass(List.class);
        verify(mapper).upsertBatch(captor.capture());
        CameraRecord record = captor.getValue().get(0);
        assertEquals("cam-1", record.getCameraId());
        assertEquals("north gate", record.getCameraName());
        assertEquals(30.1d, record.getLatitude());
        assertEquals(120.2d, record.getLongitude());
        assertEquals(1, record.getStatus());
        assertNotNull(record.getCreatedAt());
        assertNotNull(record.getUpdatedAt());
    }
}
