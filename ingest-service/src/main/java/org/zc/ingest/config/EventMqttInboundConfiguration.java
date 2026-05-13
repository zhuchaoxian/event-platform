package org.zc.ingest.config;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.ExecutorChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.StringUtils;

@Configuration
@ConditionalOnProperty(prefix = "event.ingest.event-mqtt", name = "enabled", havingValue = "true")
public class EventMqttInboundConfiguration {
    public static final String MQTT_INPUT_CHANNEL = "mqttInputChannel";

    @Bean
    public MqttPahoClientFactory mqttClientFactory(EventIngestProperties properties) {
        EventIngestProperties.EventMqtt mqtt = properties.getEventMqtt();
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[] { mqtt.getUrl() });
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        if (StringUtils.hasText(mqtt.getUsername())) {
            options.setUserName(mqtt.getUsername());
        }
        if (StringUtils.hasText(mqtt.getPassword())) {
            options.setPassword(mqtt.getPassword().toCharArray());
        }

        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        factory.setConnectionOptions(options);
        return factory;
    }

//    @Bean(name = MQTT_INPUT_CHANNEL)
//    public MessageChannel mqttInputChannel() {
//        return new DirectChannel();
//    }

    @Bean(name = MQTT_INPUT_CHANNEL)
    public MessageChannel mqttInputChannel() {
        ThreadPoolTaskExecutor executor =
                new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(16);
        executor.setMaxPoolSize(64);
        executor.setQueueCapacity(10000);
        executor.setThreadNamePrefix("mqtt-consumer-");
        executor.initialize();
        return new ExecutorChannel(executor);
    }

    @Bean
    public MessageProducer mqttInboundAdapter(
        EventIngestProperties properties,
        MqttPahoClientFactory mqttPahoClientFactory,
        MessageChannel mqttInputChannel
    ) {
        EventIngestProperties.EventMqtt mqtt = properties.getEventMqtt();
        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(
            mqtt.getUrl(),
            mqtt.getClientId(),
            mqttPahoClientFactory,
            mqtt.getTopics().toArray(new String[0])
        );
        DefaultPahoMessageConverter converter = new DefaultPahoMessageConverter();
        converter.setPayloadAsBytes(false);
        adapter.setConverter(converter);
        adapter.setCompletionTimeout(5000);
        adapter.setQos(resolveQos(mqtt));
        adapter.setOutputChannel(mqttInputChannel);
        return adapter;
    }

    private int[] resolveQos(EventIngestProperties.EventMqtt mqtt) {
        if (mqtt.getQos().isEmpty()) {
            return new int[] { 1 };
        }
        if (mqtt.getQos().size() == 1 && mqtt.getTopics().size() > 1) {
            int qos = mqtt.getQos().get(0);
            int[] qosArray = new int[mqtt.getTopics().size()];
            for (int i = 0; i < qosArray.length; i++) {
                qosArray[i] = qos;
            }
            return qosArray;
        }
        if (mqtt.getQos().size() != mqtt.getTopics().size()) {
            throw new IllegalArgumentException("mqtt qos size must be 1 or match topics size");
        }
        return mqtt.getQos().stream().mapToInt(Integer::intValue).toArray();
    }
}
