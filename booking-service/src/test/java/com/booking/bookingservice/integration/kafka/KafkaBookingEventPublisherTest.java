package com.booking.bookingservice.integration.kafka;

import com.booking.bookingservice.base.AbstractBaseServiceTest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

class KafkaBookingEventPublisherTest extends AbstractBaseServiceTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Environment environment;

    private KafkaBookingEventPublisher publisher;

    @BeforeEach
    void setUp() {
        when(environment.getProperty("justlife.kafka.topics.booking-events", "booking.events"))
                .thenReturn("booking.events.custom");

        publisher = new KafkaBookingEventPublisher(kafkaTemplate, objectMapper, environment);
    }

    @Test
    void publish_givenValidEventAndCustomTopic_thenSerializesAndSendsToKafka() throws Exception {

        // Given
        BookingEventMessage event = bookingEventMessage();
        String payload = "{\"bookingId\":\"booking-1\"}";

        when(objectMapper.writeValueAsString(event)).thenReturn(payload);

        // When
        publisher.publish(event);

        // Then
        verify(objectMapper, times(1)).writeValueAsString(event);
        verify(kafkaTemplate, times(1))
                .send("booking.events.custom", "booking-1", payload);
    }

    @Test
    void publish_givenDefaultTopic_thenUsesDefaultTopicValue() throws Exception {

        // Given
        when(environment.getProperty("justlife.kafka.topics.booking-events", "booking.events"))
                .thenReturn("booking.events");

        KafkaBookingEventPublisher defaultTopicPublisher =
                new KafkaBookingEventPublisher(kafkaTemplate, objectMapper, environment);

        BookingEventMessage event = bookingEventMessage();
        String payload = "{\"bookingId\":\"booking-1\"}";

        when(objectMapper.writeValueAsString(event)).thenReturn(payload);

        // When
        defaultTopicPublisher.publish(event);

        // Then
        verify(objectMapper, times(1)).writeValueAsString(event);
        verify(kafkaTemplate, times(1))
                .send("booking.events", "booking-1", payload);
    }

    @Test
    void publish_givenJsonProcessingException_thenDoesNotThrowAndDoesNotSend() throws Exception {

        // Given
        BookingEventMessage event = bookingEventMessage();

        when(objectMapper.writeValueAsString(event))
                .thenThrow(new TestJsonProcessingException("serialization failed"));

        // When / Then
        assertThatCode(() -> publisher.publish(event))
                .doesNotThrowAnyException();

        verify(objectMapper, times(1)).writeValueAsString(event);
        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void publish_givenKafkaSendThrowsException_thenDoesNotThrow() throws Exception {

        // Given
        BookingEventMessage event = bookingEventMessage();
        String payload = "{\"bookingId\":\"booking-1\"}";

        when(objectMapper.writeValueAsString(event)).thenReturn(payload);
        when(kafkaTemplate.send("booking.events.custom", "booking-1", payload))
                .thenThrow(new RuntimeException("kafka unavailable"));

        // When / Then
        assertThatCode(() -> publisher.publish(event))
                .doesNotThrowAnyException();

        verify(objectMapper, times(1)).writeValueAsString(event);
        verify(kafkaTemplate, times(1))
                .send("booking.events.custom", "booking-1", payload);
    }

    private BookingEventMessage bookingEventMessage() {
        return new BookingEventMessage(
                BookingEventType.BOOKING_CREATED,
                "booking-1",
                "vehicle-1",
                OffsetDateTime.of(2026, 3, 16, 10, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 3, 16, 12, 0, 0, 0, ZoneOffset.UTC),
                2,
                List.of("cleaner-1", "cleaner-2"),
                OffsetDateTime.of(2026, 3, 16, 9, 55, 0, 0, ZoneOffset.UTC)
        );
    }

    private static final class TestJsonProcessingException extends JsonProcessingException {
        private TestJsonProcessingException(String message) {
            super(message);
        }
    }
}