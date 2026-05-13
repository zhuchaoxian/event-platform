package org.zc.ingest.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "event.ingest")
public class EventIngestProperties {
    private String eventTopic = "event-topic";
    private String cameraTopic = "camera-topic";
    private EventMqtt eventMqtt = new EventMqtt();
    private CameraMqtt cameraMqtt = new CameraMqtt();

    @Data
    public static class EventMqtt {
        private boolean enabled;
        private String url = "tcp://localhost:1883";
        private String clientId = "ingest-service-mqtt-client";
        private String username;
        private String password;
        private List<String> topics = new ArrayList<>(List.of("event/#"));
        private List<Integer> qos = new ArrayList<>(List.of(1));
    }

    @Data
    public static class CameraMqtt {
        private boolean enabled;
        private String url = "tcp://localhost:1883";
        private String clientId = "ingest-service-camera-mqtt-client";
        private String username;
        private String password;
        private List<String> topics = new ArrayList<>(List.of("camera/#"));
        private List<Integer> qos = new ArrayList<>(List.of(1));
    }
}
