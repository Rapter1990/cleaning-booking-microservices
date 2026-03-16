package com.booking.professionalservice.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaTemplateConfigTest {

    private KafkaTemplateConfig kafkaTemplateConfig;

    @Mock
    private Environment environment;

    @BeforeEach
    void setUp() {
        kafkaTemplateConfig = new KafkaTemplateConfig();
    }

    @Test
    void kafkaTemplate_givenCustomKafkaProperties_thenCreatesTemplateWithExpectedConfig() {

        // Given
        when(environment.getProperty("spring.kafka.bootstrap-servers", "localhost:9092"))
                .thenReturn("kafka:29092");
        when(environment.getProperty("spring.kafka.producer.acks", "all"))
                .thenReturn("1");

        // When
        KafkaTemplate<Object, Object> kafkaTemplate = kafkaTemplateConfig.kafkaTemplate(environment);

        // Then
        assertThat(kafkaTemplate).isNotNull();

        ProducerFactory<Object, Object> producerFactory = kafkaTemplate.getProducerFactory();
        assertThat(producerFactory).isInstanceOf(DefaultKafkaProducerFactory.class);

        @SuppressWarnings("unchecked")
        DefaultKafkaProducerFactory<Object, Object> defaultKafkaProducerFactory =
                (DefaultKafkaProducerFactory<Object, Object>) producerFactory;

        Map<String, Object> configs = defaultKafkaProducerFactory.getConfigurationProperties();

        assertThat(configs)
                .containsEntry(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:29092")
                .containsEntry(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class)
                .containsEntry(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class)
                .containsEntry(ProducerConfig.ACKS_CONFIG, "1");

        verify(environment).getProperty("spring.kafka.bootstrap-servers", "localhost:9092");
        verify(environment).getProperty("spring.kafka.producer.acks", "all");

    }

    @Test
    void kafkaTemplate_givenMissingKafkaProperties_thenUsesDefaultValues() {

        // Given
        // Mockito does not automatically apply Environment default values,
        // so we stub the exact values that Spring would return.
        when(environment.getProperty("spring.kafka.bootstrap-servers", "localhost:9092"))
                .thenReturn("localhost:9092");
        when(environment.getProperty("spring.kafka.producer.acks", "all"))
                .thenReturn("all");

        // When
        KafkaTemplate<Object, Object> kafkaTemplate = kafkaTemplateConfig.kafkaTemplate(environment);

        // Then
        assertThat(kafkaTemplate).isNotNull();

        ProducerFactory<Object, Object> producerFactory = kafkaTemplate.getProducerFactory();
        assertThat(producerFactory).isInstanceOf(DefaultKafkaProducerFactory.class);

        @SuppressWarnings("unchecked")
        DefaultKafkaProducerFactory<Object, Object> defaultKafkaProducerFactory =
                (DefaultKafkaProducerFactory<Object, Object>) producerFactory;

        Map<String, Object> configs = defaultKafkaProducerFactory.getConfigurationProperties();

        assertThat(configs)
                .containsEntry(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
                .containsEntry(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class)
                .containsEntry(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class)
                .containsEntry(ProducerConfig.ACKS_CONFIG, "all");

        verify(environment).getProperty("spring.kafka.bootstrap-servers", "localhost:9092");
        verify(environment).getProperty("spring.kafka.producer.acks", "all");

    }

}