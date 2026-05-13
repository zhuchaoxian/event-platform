package org.zc.storage.sentinel;

import java.util.List;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import org.springframework.stereotype.Component;
import org.zc.common.Event;
import org.zc.storage.dao.EventDao;

@Component
public class EventSentinelFacade {

    public static final String PERSIST_RESOURCE = "event-persist";

    @SentinelResource(value = PERSIST_RESOURCE, blockHandler = "handlePersistBatchBlock")
    public int persistBatch(EventDao eventDao, List<Event> events) {
        return eventDao.saveBatch(events);
    }

    public int handlePersistBatchBlock(
            EventDao eventDao,
            List<Event> events,
            BlockException exception) {
        throw new EventSentinelBlockedException("sentinel persist blocked", exception);
    }
}
