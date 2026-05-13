package org.zc.consumer.consumer;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

import org.junit.jupiter.api.Test;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.zc.common.Event;
import org.zc.consumer.service.EventFailurePublisher;
import org.zc.consumer.service.EventProcessingService;

class EventKafkaConsumerTest {

    @Test
    void shouldDelegateEventToAsyncProcessor() {
        EventProcessingService processingService = mock(EventProcessingService.class);
        EventFailurePublisher failurePublisher = mock(EventFailurePublisher.class);
        EventKafkaConsumer consumer = new EventKafkaConsumer(new SyncTaskExecutor(), processingService, failurePublisher);

        Event event = buildEvent();

        consumer.onMessage(event, event.getDeviceId());

        verify(processingService).process(event);
    }

    @Test
    void shouldPublishFailureWhenExecutorRejected() {
        TaskExecutor rejectingExecutor = task -> {
            throw new RejectedExecutionException("queue full");
        };
        EventProcessingService processingService = mock(EventProcessingService.class);
        EventFailurePublisher failurePublisher = mock(EventFailurePublisher.class);
        EventKafkaConsumer consumer = new EventKafkaConsumer(rejectingExecutor, processingService, failurePublisher);

        Event event = buildEvent();
        consumer.onMessage(event, event.getDeviceId());

        verify(failurePublisher).publish(eq(event), eq("executor"), eq("queue full"), eq(0));
    }

    @Test
    void shouldPublishFailureWhenAsyncProcessingFails() {
        EventProcessingService processingService = mock(EventProcessingService.class);
        doThrow(new IllegalStateException("dispatch failed")).when(processingService).process(org.mockito.ArgumentMatchers.any());
        EventFailurePublisher failurePublisher = mock(EventFailurePublisher.class);
        EventKafkaConsumer consumer = new EventKafkaConsumer(new SyncTaskExecutor(), processingService, failurePublisher);

        Event event = buildEvent();
        consumer.onMessage(event, event.getDeviceId());

        verify(failurePublisher).publish(eq(event), eq("async-process"), eq("dispatch failed"), eq(0));
    }

    private Event buildEvent() {
        Event event = new Event();
        event.setEventId("evt-1");
        event.setDeviceId("device-1");
        event.setType("temperature");
        event.setPayload(Map.of("value", 21));
        return event;
    }
}
