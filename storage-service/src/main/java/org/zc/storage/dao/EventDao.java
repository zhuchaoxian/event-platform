package org.zc.storage.dao;

import java.util.List;

import org.zc.common.Event;

public interface EventDao {

    int saveBatch(List<Event> events);
}
