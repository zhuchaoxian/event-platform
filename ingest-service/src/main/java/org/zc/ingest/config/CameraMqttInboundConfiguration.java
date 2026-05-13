package org.zc.ingest.config;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.StringUtils;

@Configuration
@ConditionalOnProperty(prefix = "event.ingest.camera-mqtt", name = "enabled", havingValue = "true")
public class CameraMqttInboundConfiguration {

    public static final String CAMERA_MQTT_INPUT_CHANNEL = "cameraMqttInputChannel";

    @Bean
    public MqttPahoClientFactory cameraMqttClientFactory(EventIngestProperties properties) {
        EventIngestProperties.CameraMqtt mqtt = properties.getCameraMqtt();
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

    @Bean(name = CAMERA_MQTT_INPUT_CHANNEL)
    public MessageChannel cameraMqttInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageProducer cameraMqttInboundAdapter(
            EventIngestProperties properties,
            MqttPahoClientFactory cameraMqttClientFactory,
            MessageChannel cameraMqttInputChannel) {
        EventIngestProperties.CameraMqtt mqtt = properties.getCameraMqtt();
        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(
                mqtt.getUrl(),
                mqtt.getClientId(),
                cameraMqttClientFactory,
                mqtt.getTopics().toArray(new String[0]));
        DefaultPahoMessageConverter converter = new DefaultPahoMessageConverter();
        converter.setPayloadAsBytes(false);
        adapter.setConverter(converter);
        adapter.setCompletionTimeout(5000);
        adapter.setQos(resolveQos(mqtt));
        adapter.setOutputChannel(cameraMqttInputChannel);
        return adapter;
    }

    private int[] resolveQos(EventIngestProperties.CameraMqtt mqtt) {
        if (mqtt.getQos().isEmpty()) {
            return new int[] { 1 };
        }
        if (mqtt.getQos().size() == 1 && mqtt.getTopics().size() > 1) {
            int qos = mqtt.getQos().get(0);
            int[] qosArray = new int[mqtt.getTopics().size()];
            for (int index = 0; index < qosArray.length; index++) {
                qosArray[index] = qos;
            }
            return qosArray;
        }
        if (mqtt.getQos().size() != mqtt.getTopics().size()) {
            throw new IllegalArgumentException("camera mqtt qos size must be 1 or match topics size");
        }
        return mqtt.getQos().stream().mapToInt(Integer::intValue).toArray();
    }
}
