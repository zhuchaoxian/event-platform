package org.zc.storage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "camera")
public class CameraProperties {

    private String topic = "camera-topic";
    private String failureTopic = "camera-failure-topic";
    private int queueCapacity = 200;
    private int workerCount = 2;
    private int batchSize = 200;
    private long pollTimeoutMs = 500L;
    private int enqueueRetryTimes = 3;
    private long enqueueRetryBackoffMs = 100L;
    private int dbRetryTimes = 3;
    private Sentinel sentinel = new Sentinel();

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getFailureTopic() {
        return failureTopic;
    }

    public void setFailureTopic(String failureTopic) {
        this.failureTopic = failureTopic;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    public int getWorkerCount() {
        return workerCount;
    }

    public void setWorkerCount(int workerCount) {
        this.workerCount = workerCount;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public long getPollTimeoutMs() {
        return pollTimeoutMs;
    }

    public void setPollTimeoutMs(long pollTimeoutMs) {
        this.pollTimeoutMs = pollTimeoutMs;
    }

    public int getEnqueueRetryTimes() {
        return enqueueRetryTimes;
    }

    public void setEnqueueRetryTimes(int enqueueRetryTimes) {
        this.enqueueRetryTimes = enqueueRetryTimes;
    }

    public long getEnqueueRetryBackoffMs() {
        return enqueueRetryBackoffMs;
    }

    public void setEnqueueRetryBackoffMs(long enqueueRetryBackoffMs) {
        this.enqueueRetryBackoffMs = enqueueRetryBackoffMs;
    }

    public int getDbRetryTimes() {
        return dbRetryTimes;
    }

    public void setDbRetryTimes(int dbRetryTimes) {
        this.dbRetryTimes = dbRetryTimes;
    }

    public Sentinel getSentinel() {
        return sentinel;
    }

    public void setSentinel(Sentinel sentinel) {
        this.sentinel = sentinel;
    }

    public static class Sentinel {

        private boolean enabled = true;
        private double enqueueQps = 200d;
        private double persistQps = 100d;
        private double persistExceptionCount = 5d;
        private int degradeWindowSeconds = 10;
        private int minRequestAmount = 5;
        private int statIntervalMs = 10000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public double getEnqueueQps() {
            return enqueueQps;
        }

        public void setEnqueueQps(double enqueueQps) {
            this.enqueueQps = enqueueQps;
        }

        public double getPersistQps() {
            return persistQps;
        }

        public void setPersistQps(double persistQps) {
            this.persistQps = persistQps;
        }

        public double getPersistExceptionCount() {
            return persistExceptionCount;
        }

        public void setPersistExceptionCount(double persistExceptionCount) {
            this.persistExceptionCount = persistExceptionCount;
        }

        public int getDegradeWindowSeconds() {
            return degradeWindowSeconds;
        }

        public void setDegradeWindowSeconds(int degradeWindowSeconds) {
            this.degradeWindowSeconds = degradeWindowSeconds;
        }

        public int getMinRequestAmount() {
            return minRequestAmount;
        }

        public void setMinRequestAmount(int minRequestAmount) {
            this.minRequestAmount = minRequestAmount;
        }

        public int getStatIntervalMs() {
            return statIntervalMs;
        }

        public void setStatIntervalMs(int statIntervalMs) {
            this.statIntervalMs = statIntervalMs;
        }
    }
}
