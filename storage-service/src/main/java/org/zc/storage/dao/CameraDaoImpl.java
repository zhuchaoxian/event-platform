package org.zc.storage.dao;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;
import org.zc.common.CameraMessage;
import org.zc.storage.entity.CameraRecord;
import org.zc.storage.mapper.CameraMapper;

@Repository
public class CameraDaoImpl implements CameraDao {

    private final CameraMapper cameraMapper;

    public CameraDaoImpl(CameraMapper cameraMapper) {
        this.cameraMapper = cameraMapper;
    }

    @Override
    public int saveBatch(List<CameraMessage> cameraMessages) {
        List<CameraRecord> records = new ArrayList<>(cameraMessages.size());
        for (CameraMessage cameraMessage : cameraMessages) {
            if (cameraMessage != null) {
                records.add(toRecord(cameraMessage));
            }
        }
        if (records.isEmpty()) {
            return 0;
        }
        return cameraMapper.upsertBatch(records);
    }

    private CameraRecord toRecord(CameraMessage cameraMessage) {
        LocalDateTime now = LocalDateTime.now();
        CameraRecord record = new CameraRecord();
        record.setCameraId(cameraMessage.getCameraId());
        record.setCameraName(cameraMessage.getCameraName());
        record.setLatitude(cameraMessage.getLatitude());
        record.setLongitude(cameraMessage.getLongitude());
        record.setStatus(cameraMessage.getStatus());
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        return record;
    }
}
