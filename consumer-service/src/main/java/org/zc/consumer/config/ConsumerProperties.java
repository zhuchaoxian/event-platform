package org.zc.consumer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "event.consumer")
public class ConsumerProperties {

    private String topic = "event-topic";
    private String storageTopic = "event-storage-topic";
    private String failureTopic = "event-failure-topic";

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getStorageTopic() {
        return storageTopic;
    }

    public void setStorageTopic(String storageTopic) {
        this.storageTopic = storageTopic;
    }

    public String getFailureTopic() {
        return failureTopic;
    }

    public void setFailureTopic(String failureTopic) {
        this.failureTopic = failureTopic;
    }
}
