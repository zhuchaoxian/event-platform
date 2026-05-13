package org.zc.storage.dao;

import java.util.List;

import org.zc.common.CameraMessage;

public interface CameraDao {

    int saveBatch(List<CameraMessage> cameraMessages);
}
