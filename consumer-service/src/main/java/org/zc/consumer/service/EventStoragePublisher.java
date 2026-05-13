package org.zc.consumer.service;

import org.zc.common.Event;

public interface EventStoragePublisher {

    void publish(Event event);
}
