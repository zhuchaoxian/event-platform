package org.zc.ingest.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.zc.common.Event;
import org.zc.ingest.config.EventIngestProperties;

import java.util.ArrayDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventKafkaProducer {
    private final KafkaTemplate<String, Event> kafkaTemplate;
    private final EventIngestProperties eventIngestProperties;
    /**
     * 重试队列
     */
    private final BlockingQueue<Event> retryQueue =
            new LinkedBlockingQueue<>(100000);
    public void send(Event event) {
        kafkaTemplate.send(eventIngestProperties.getEventTopic(), event.getDeviceId(), event).whenComplete((r, e) -> {

            if (e != null) {

                // 放入失败队列
                retryQueue.offer(event);

                log.error("kafka发送失败", e);
            }
        });
        log.info(
            "event published to kafka. topic={}, eventId={}, deviceId={}, type={}",
            eventIngestProperties.getEventTopic(),
            event.getEventId(),
            event.getDeviceId(),
            event.getType()
        );
    }

    @Scheduled(fixedDelay = 1000)
    public void retry() {

        while (true) {

            Event msg = retryQueue.poll();

            if (msg == null) {
                break;
            }

            try {
                kafkaTemplate.send(eventIngestProperties.getEventTopic(), msg).get();

            } catch (Exception e) {

                // 重新放回
                retryQueue.offer(msg);

                break;
            }
        }
    }
}
