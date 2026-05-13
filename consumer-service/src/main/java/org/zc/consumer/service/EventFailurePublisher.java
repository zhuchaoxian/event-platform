package org.zc.consumer.service;

import org.zc.common.Event;

public interface EventFailurePublisher {

    void publish(Event event, String stage, String reason, int attempts);
}
