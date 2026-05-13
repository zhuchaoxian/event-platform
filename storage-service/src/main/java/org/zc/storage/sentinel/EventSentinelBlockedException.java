package org.zc.storage.sentinel;

public class EventSentinelBlockedException extends RuntimeException {

    public EventSentinelBlockedException(String message, Throwable cause) {
        super(message, cause);
    }
}
