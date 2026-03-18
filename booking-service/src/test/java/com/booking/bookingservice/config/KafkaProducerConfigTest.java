package com.booking.bookingservice.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class KafkaProducerConfigTest {

    private KafkaProducerConfig kafkaProducerConfig;
    private KafkaProperties kafkaProperties;

    @BeforeEach
    void setUp() {
        kafkaProducerConfig = new KafkaProducerConfig();
        kafkaProperties = mock(KafkaProperties.class);
    }

    @Test
    void producerFactory_givenKafkaProperties_thenCreatesProducerFactoryWithStringSerializers() {
        // Given
        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        producerProps.put(ProducerConfig.ACKS_CONFIG, "all");

        when(kafkaProperties.buildProducerProperties()).thenReturn(producerProps);

        // When
        ProducerFactory<String, String> producerFactory = kafkaProducerConfig.producerFactory(kafkaProperties);

        // Then
        assertThat(producerFactory).isNotNull();
        assertThat(producerFactory).isInstanceOf(DefaultKafkaProducerFactory.class);

        @SuppressWarnings("unchecked")
        DefaultKafkaProducerFactory<String, String> defaultProducerFactory =
                (DefaultKafkaProducerFactory<String, String>) producerFactory;

        Map<String, Object> configMap = defaultProducerFactory.getConfigurationProperties();

        assertThat(configMap)
                .containsEntry(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
                .containsEntry(ProducerConfig.ACKS_CONFIG, "all")
                .containsEntry(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class)
                .containsEntry(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        verify(kafkaProperties, times(1)).buildProducerProperties();
    }

    @Test
    void producerFactory_givenExistingSerializerProperties_thenOverridesWithStringSerializers() {
        // Given
        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:29092");
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, Object.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, Object.class);

        when(kafkaProperties.buildProducerProperties()).thenReturn(producerProps);

        // When
        ProducerFactory<String, String> producerFactory = kafkaProducerConfig.producerFactory(kafkaProperties);

        // Then
        @SuppressWarnings("unchecked")
        DefaultKafkaProducerFactory<String, String> defaultProducerFactory =
                (DefaultKafkaProducerFactory<String, String>) producerFactory;

        Map<String, Object> configMap = defaultProducerFactory.getConfigurationProperties();

        assertThat(configMap)
                .containsEntry(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:29092")
                .containsEntry(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class)
                .containsEntry(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    }

    @Test
    void kafkaTemplate_givenProducerFactory_thenCreatesKafkaTemplate() {
        // Given
        ProducerFactory<String, String> producerFactory = mock(ProducerFactory.class);

        // When
        KafkaTemplate<String, String> kafkaTemplate = kafkaProducerConfig.kafkaTemplate(producerFactory);

        // Then
        assertThat(kafkaTemplate).isNotNull();
        assertThat(kafkaTemplate.getProducerFactory()).isSameAs(producerFactory);
    }

}