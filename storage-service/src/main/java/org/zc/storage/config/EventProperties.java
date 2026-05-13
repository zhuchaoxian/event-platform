package org.zc.storage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "event")
public class EventProperties {

    private String topic = "event-storage-topic";
    private String failureTopic = "event-failure-topic";
    private int retryTimes = 3;
    private int batchSize = 200;
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

    public int getRetryTimes() {
        return retryTimes;
    }

    public void setRetryTimes(int retryTimes) {
        this.retryTimes = retryTimes;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
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
