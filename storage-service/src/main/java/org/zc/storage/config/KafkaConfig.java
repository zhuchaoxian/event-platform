package org.zc.storage.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.zc.common.CameraMessage;
import org.zc.common.Event;

@Configuration
@EnableConfigurationProperties({EventProperties.class, CameraProperties.class})
public class KafkaConfig {

    @Bean
    public ConsumerFactory<String, Event> eventConsumerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> properties = consumerProperties(kafkaProperties);
        JsonDeserializer<Event> deserializer = new JsonDeserializer<>(Event.class, false);
        deserializer.addTrustedPackages("org.zc.common");
        deserializer.setUseTypeHeaders(false);
        return new DefaultKafkaConsumerFactory<>(properties, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Event> eventKafkaListenerContainerFactory(
            ConsumerFactory<String, Event> eventConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, Event> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(eventConsumerFactory);
        factory.setBatchListener(true);
        return factory;
    }

    @Bean
    public ConsumerFactory<String, CameraMessage> cameraConsumerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> properties = consumerProperties(kafkaProperties);
        JsonDeserializer<CameraMessage> deserializer = new JsonDeserializer<>(CameraMessage.class, false);
        deserializer.addTrustedPackages("org.zc.common");
        deserializer.setUseTypeHeaders(false);
        return new DefaultKafkaConsumerFactory<>(properties, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CameraMessage> cameraKafkaListenerContainerFactory(
            ConsumerFactory<String, CameraMessage> cameraConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, CameraMessage> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(cameraConsumerFactory);
        factory.setBatchListener(false);
        return factory;
    }

    private Map<String, Object> consumerProperties(KafkaProperties kafkaProperties) {
        Map<String, Object> properties = new HashMap<>(kafkaProperties.buildConsumerProperties(null));
        properties.remove(JsonDeserializer.TRUSTED_PACKAGES);
        properties.remove(JsonDeserializer.VALUE_DEFAULT_TYPE);
        properties.remove(JsonDeserializer.USE_TYPE_INFO_HEADERS);
        properties.remove(JsonDeserializer.REMOVE_TYPE_INFO_HEADERS);
        return properties;
    }
}
