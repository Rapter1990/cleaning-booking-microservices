package com.booking.professionalservice.integration.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.listener.RetryListener;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class KafkaDlqConfigTest {

    private KafkaDlqConfig kafkaDlqConfig;
    private KafkaTemplate<Object, Object> kafkaTemplate;
    private Environment environment;

    @BeforeEach
    void setUp() {
        this.kafkaDlqConfig = new KafkaDlqConfig();
        this.kafkaTemplate = mock(KafkaTemplate.class);
        this.environment = mock(Environment.class);

        when(kafkaTemplate.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void kafkaCommonErrorHandler_givenConfiguredDltTopic_thenPublishesToConfiguredTopic() {
        // Given
        when(environment.getProperty("justlife.kafka.topics.booking-events-dlt"))
                .thenReturn("booking.events.custom.dlt");

        DefaultErrorHandler handler = buildHandler();
        handler.setSeekAfterError(false);

        ConsumerRecord<String, String> failedRecord =
                new ConsumerRecord<>("booking.events", 2, 15L, "key-1", "payload-1");

        Consumer<?, ?> consumer = mock(Consumer.class);
        MessageListenerContainer container = mock(MessageListenerContainer.class);

        // When
        handler.handleOne(new IllegalArgumentException("boom"), failedRecord, consumer, container);

        // Then
        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<ProducerRecord<Object, Object>> captor =
                org.mockito.ArgumentCaptor.forClass((Class) ProducerRecord.class);

        verify(kafkaTemplate, times(1)).send(captor.capture());

        ProducerRecord<Object, Object> published = captor.getValue();
        assertThat(published.topic()).isEqualTo("booking.events.custom.dlt");
        assertThat(published.key()).isEqualTo("key-1");

        verify(environment, times(1))
                .getProperty("justlife.kafka.topics.booking-events-dlt");
    }

    @Test
    void kafkaCommonErrorHandler_givenBlankConfiguredDltTopic_thenFallsBackToOriginalTopicDotDlt() {
        // Given
        when(environment.getProperty("justlife.kafka.topics.booking-events-dlt"))
                .thenReturn("   ");

        DefaultErrorHandler handler = buildHandler();
        handler.setSeekAfterError(false);

        ConsumerRecord<String, String> failedRecord =
                new ConsumerRecord<>("booking.events", 1, 7L, "key-2", "payload-2");

        Consumer<?, ?> consumer = mock(Consumer.class);
        MessageListenerContainer container = mock(MessageListenerContainer.class);

        // When
        handler.handleOne(new IllegalArgumentException("boom"), failedRecord, consumer, container);

        // Then
        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<ProducerRecord<Object, Object>> captor =
                org.mockito.ArgumentCaptor.forClass((Class) ProducerRecord.class);

        verify(kafkaTemplate, times(1)).send(captor.capture());

        ProducerRecord<Object, Object> published = captor.getValue();
        assertThat(published.topic()).isEqualTo("booking.events.dlt");
        assertThat(published.key()).isEqualTo("key-2");
    }

    @Test
    void kafkaCommonErrorHandler_givenNullConfiguredDltTopic_andJsonProcessingException_thenFallsBackToOriginalTopicDotDlt() {
        // Given
        when(environment.getProperty("justlife.kafka.topics.booking-events-dlt"))
                .thenReturn(null);

        DefaultErrorHandler handler = buildHandler();
        handler.setSeekAfterError(false);

        ConsumerRecord<String, String> failedRecord =
                new ConsumerRecord<>("booking.events", 0, 3L, "key-3", "payload-3");

        Consumer<?, ?> consumer = mock(Consumer.class);
        MessageListenerContainer container = mock(MessageListenerContainer.class);

        // When
        handler.handleOne(new TestJsonProcessingException("bad-json"), failedRecord, consumer, container);

        // Then
        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<ProducerRecord<Object, Object>> captor =
                org.mockito.ArgumentCaptor.forClass((Class) ProducerRecord.class);

        verify(kafkaTemplate, times(1)).send(captor.capture());

        ProducerRecord<Object, Object> published = captor.getValue();
        assertThat(published.topic()).isEqualTo("booking.events.dlt");
        assertThat(published.key()).isEqualTo("key-3");
    }

    @Test
    void kafkaCommonErrorHandler_thenRegistersRetryListener() {
        // Given
        when(environment.getProperty("justlife.kafka.topics.booking-events-dlt"))
                .thenReturn("booking.events.custom.dlt");

        DefaultErrorHandler handler = buildHandler();

        // When
        @SuppressWarnings("unchecked")
        List<RetryListener> retryListeners =
                (List<RetryListener>) ReflectionTestUtils.invokeMethod(handler, "getRetryListeners");

        // Then
        assertThat(retryListeners).isNotNull();
        assertThat(retryListeners).hasSize(1);

        ConsumerRecord<String, String> failedRecord =
                new ConsumerRecord<>("booking.events", 3, 11L, "key-4", "payload-4");

        assertThatCode(() ->
                retryListeners.get(0).failedDelivery(failedRecord, new RuntimeException("retry"), 1)
        ).doesNotThrowAnyException();
    }

    private DefaultErrorHandler buildHandler() {
        CommonErrorHandler commonErrorHandler =
                kafkaDlqConfig.kafkaCommonErrorHandler(kafkaTemplate, environment);

        assertThat(commonErrorHandler).isInstanceOf(DefaultErrorHandler.class);
        return (DefaultErrorHandler) commonErrorHandler;
    }

    private static final class TestJsonProcessingException extends JsonProcessingException {
        private TestJsonProcessingException(String message) {
            super(message);
        }
    }
}