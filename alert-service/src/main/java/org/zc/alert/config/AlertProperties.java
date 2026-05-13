package org.zc.alert.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "alert")
public class AlertProperties {
    private Kafka kafka = new Kafka();
    private Cooldown cooldown = new Cooldown();
    private Frequency frequency = new Frequency();

    @Data
    public static class Kafka {
        private String topic = "event-topic";
        private String consumerGroup = "alert-service-group";
        private String notificationTopic = "alert-notification-topic";
    }

    @Data
    public static class Cooldown {
        private int defaultSeconds = 300;
    }

    @Data
    public static class Frequency {
        private int windowSeconds = 60;
        private int defaultMaxCount = 10;
    }
}
