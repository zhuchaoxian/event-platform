package org.zc.storage.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.zc.common.CameraMessage;
import org.zc.storage.config.CameraProperties;
import org.zc.storage.sentinel.CameraSentinelBlockedException;
import org.zc.storage.sentinel.CameraSentinelFacade;

@Service
public class CameraDispatcherService {

    private static final Logger log = LoggerFactory.getLogger(CameraDispatcherService.class);

    private final CameraProperties cameraProperties;
    private final CameraFailurePublisher cameraFailurePublisher;
    private final CameraPersistenceService cameraPersistenceService;
    private final CameraSentinelFacade sentinelFacade;
    private final BlockingQueue<CameraMessage> queue;
    private final ExecutorService workerExecutor;

    private volatile boolean running = true;

    public CameraDispatcherService(
            CameraProperties cameraProperties,
            CameraFailurePublisher cameraFailurePublisher,
            CameraPersistenceService cameraPersistenceService,
            CameraSentinelFacade sentinelFacade) {
        this.cameraProperties = cameraProperties;
        this.cameraFailurePublisher = cameraFailurePublisher;
        this.cameraPersistenceService = cameraPersistenceService;
        this.sentinelFacade = sentinelFacade;
        int queueCapacity = Math.max(1, cameraProperties.getQueueCapacity());
        int workerCount = Math.max(1, cameraProperties.getWorkerCount());
        this.queue = new ArrayBlockingQueue<>(queueCapacity);
        this.workerExecutor = Executors.newFixedThreadPool(workerCount, new CameraWorkerThreadFactory());
    }

    @PostConstruct
    public void startWorkers() {
        int workerCount = Math.max(1, cameraProperties.getWorkerCount());
        for (int i = 0; i < workerCount; i++) {
            workerExecutor.submit(this::runWorker);
        }
    }

    @PreDestroy
    public void shutdown() {
        running = false;
        workerExecutor.shutdownNow();
        try {
            if (!workerExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("Camera storage worker pool did not stop within timeout");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    public void submit(CameraMessage cameraMessage) {
        int retryTimes = Math.max(1, cameraProperties.getEnqueueRetryTimes());
        long backoffMs = Math.max(0L, cameraProperties.getEnqueueRetryBackoffMs());
        String reason = "queue full";

        for (int attempt = 1; attempt <= retryTimes; attempt++) {
            try {
                boolean offered = sentinelFacade.offer(queue, cameraMessage);
                if (offered) {
                    log.info("Camera message enqueued for async persistence. cameraId={}, queueSize={}, attempt={}",
                            cameraMessage == null ? null : cameraMessage.getCameraId(),
                            queue.size(),
                            attempt);
                    return;
                }
            } catch (CameraSentinelBlockedException exception) {
                reason = exception.getMessage();
                log.warn("Camera enqueue blocked by Sentinel. cameraId={}, attempt={}/{}",
                        cameraMessage == null ? null : cameraMessage.getCameraId(),
                        attempt,
                        retryTimes,
                        exception);
            } catch (Exception exception) {
                reason = buildReason(exception);
                log.warn("Camera enqueue failed unexpectedly. cameraId={}, attempt={}/{}",
                        cameraMessage == null ? null : cameraMessage.getCameraId(),
                        attempt,
                        retryTimes,
                        exception);
            }

            if (attempt < retryTimes && !sleepBeforeRetry(backoffMs)) {
                reason = "interrupted while retrying enqueue";
                break;
            }
        }

        cameraFailurePublisher.publish(cameraMessage, "camera-enqueue", reason, retryTimes);
    }

    private void runWorker() {
        int batchSize = Math.max(1, cameraProperties.getBatchSize());
        long pollTimeoutMs = Math.max(1L, cameraProperties.getPollTimeoutMs());
        while (running || !queue.isEmpty()) {
            try {
                CameraMessage firstMessage = queue.poll(pollTimeoutMs, TimeUnit.MILLISECONDS);
                if (firstMessage != null) {
                    List<CameraMessage> batch = new ArrayList<>(batchSize);
                    batch.add(firstMessage);
                    queue.drainTo(batch, batchSize - 1);
                    cameraPersistenceService.persistBatch(batch);
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception exception) {
                log.error("Camera storage worker terminated due to unexpected error", exception);
            }
        }
    }

    private boolean sleepBeforeRetry(long backoffMs) {
        if (backoffMs <= 0) {
            return true;
        }
        try {
            Thread.sleep(backoffMs);
            return true;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private String buildReason(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    private static final class CameraWorkerThreadFactory implements ThreadFactory {

        private final AtomicInteger sequence = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable);
            thread.setName("camera-storage-worker-" + sequence.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}
