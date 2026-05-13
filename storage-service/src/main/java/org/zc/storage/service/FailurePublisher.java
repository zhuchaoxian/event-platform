package org.zc.storage.service;

import org.zc.common.Event;

public interface FailurePublisher {

    void publish(Event event, String stage, String reason, int attempts);
}
