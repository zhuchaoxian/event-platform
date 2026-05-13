package org.zc.storage.sentinel;

import java.util.List;
import java.util.concurrent.BlockingQueue;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import org.springframework.stereotype.Component;
import org.zc.common.CameraMessage;
import org.zc.storage.dao.CameraDao;

@Component
public class CameraSentinelFacade {

    public static final String ENQUEUE_RESOURCE = "camera-enqueue";
    public static final String PERSIST_RESOURCE = "camera-persist";

    @SentinelResource(value = ENQUEUE_RESOURCE, blockHandler = "handleOfferBlock")
    public boolean offer(BlockingQueue<CameraMessage> queue, CameraMessage cameraMessage) {
        return queue.offer(cameraMessage);
    }

    @SentinelResource(value = PERSIST_RESOURCE, blockHandler = "handlePersistBatchBlock")
    public int persistBatch(CameraDao cameraDao, List<CameraMessage> cameraMessages) {
        return cameraDao.saveBatch(cameraMessages);
    }

    public boolean handleOfferBlock(
            BlockingQueue<CameraMessage> queue,
            CameraMessage cameraMessage,
            BlockException exception) {
        throw new CameraSentinelBlockedException("sentinel enqueue blocked", exception);
    }

    public int handlePersistBatchBlock(
            CameraDao cameraDao,
            List<CameraMessage> cameraMessages,
            BlockException exception) {
        throw new CameraSentinelBlockedException("sentinel persist blocked", exception);
    }
}
