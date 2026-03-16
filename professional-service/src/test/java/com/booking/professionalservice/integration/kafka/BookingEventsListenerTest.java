package com.booking.professionalservice.integration.kafka;

import com.booking.professionalservice.base.AbstractBaseServiceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;


class BookingEventsListenerTest extends AbstractBaseServiceTest {

    @Mock
    private CacheManager cacheManager;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Cache vehiclesCache;

    @Mock
    private Cache cleanersByVehicleCache;

    @Mock
    private Cache cleanersCache;

    private BookingEventsListener bookingEventsListener;

    @BeforeEach
    void setUp() {
        bookingEventsListener = new BookingEventsListener(cacheManager, objectMapper);
    }

    @Test
    void onMessage_givenValidPayload_thenClearsAllCaches() throws Exception {

        // Given
        String payload = "{\"type\":\"ANY\",\"bookingId\":\"11111111-1111-1111-1111-111111111111\"}";
        BookingEventMessage event = validEvent();

        when(objectMapper.readValue(payload, BookingEventMessage.class)).thenReturn(event);
        when(cacheManager.getCache("professionals-vehicles")).thenReturn(vehiclesCache);
        when(cacheManager.getCache("professionals-cleaners-by-vehicle")).thenReturn(cleanersByVehicleCache);
        when(cacheManager.getCache("professionals-cleaners")).thenReturn(cleanersCache);

        // When / Then
        assertThatCode(() -> bookingEventsListener.onMessage(payload))
                .doesNotThrowAnyException();

        verify(objectMapper, times(1)).readValue(payload, BookingEventMessage.class);

        verify(cacheManager, times(1)).getCache("professionals-vehicles");
        verify(cacheManager, times(1)).getCache("professionals-cleaners-by-vehicle");
        verify(cacheManager, times(1)).getCache("professionals-cleaners");

        verify(vehiclesCache, times(1)).clear();
        verify(cleanersByVehicleCache, times(1)).clear();
        verify(cleanersCache, times(1)).clear();

    }

    @Test
    void onMessage_givenValidPayloadAndOneMissingCache_thenClearsOnlyExistingCaches() throws Exception {

        // Given
        String payload = "{\"type\":\"ANY\",\"bookingId\":\"11111111-1111-1111-1111-111111111111\"}";
        BookingEventMessage event = validEvent();

        when(objectMapper.readValue(payload, BookingEventMessage.class)).thenReturn(event);
        when(cacheManager.getCache("professionals-vehicles")).thenReturn(vehiclesCache);
        when(cacheManager.getCache("professionals-cleaners-by-vehicle")).thenReturn(null);
        when(cacheManager.getCache("professionals-cleaners")).thenReturn(cleanersCache);

        // When / Then
        assertThatCode(() -> bookingEventsListener.onMessage(payload))
                .doesNotThrowAnyException();

        verify(objectMapper, times(1)).readValue(payload, BookingEventMessage.class);

        verify(cacheManager, times(1)).getCache("professionals-vehicles");
        verify(cacheManager, times(1)).getCache("professionals-cleaners-by-vehicle");
        verify(cacheManager, times(1)).getCache("professionals-cleaners");

        verify(vehiclesCache, times(1)).clear();
        verify(cleanersCache, times(1)).clear();
        verifyNoInteractions(cleanersByVehicleCache);

    }

    @Test
    void onMessage_givenMalformedPayload_thenThrowsIllegalArgumentException_andDoesNotClearCaches() throws Exception {

        // Given
        String payload = "invalid-json";
        RuntimeException parseException = new RuntimeException("JSON parse error");

        when(objectMapper.readValue(payload, BookingEventMessage.class)).thenThrow(parseException);

        // When / Then
        assertThatThrownBy(() -> bookingEventsListener.onMessage(payload))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid booking event payload")
                .hasCause(parseException);

        verify(objectMapper, times(1)).readValue(payload, BookingEventMessage.class);
        verifyNoInteractions(cacheManager);
        verifyNoInteractions(vehiclesCache, cleanersByVehicleCache, cleanersCache);

    }

    @Test
    void onMessage_givenEventWithNullType_thenThrowsIllegalArgumentException_andDoesNotClearCaches() throws Exception {

        // Given
        String payload = "{\"bookingId\":\"11111111-1111-1111-1111-111111111111\"}";
        BookingEventMessage event = new BookingEventMessage(
                null,
                UUID.randomUUID(),
                UUID.randomUUID(),
                OffsetDateTime.now(),
                OffsetDateTime.now().plusHours(2),
                2,
                List.of(UUID.randomUUID(), UUID.randomUUID()),
                OffsetDateTime.now()
        );

        when(objectMapper.readValue(payload, BookingEventMessage.class)).thenReturn(event);

        // When / Then
        assertThatThrownBy(() -> bookingEventsListener.onMessage(payload))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid booking event payload")
                .hasCauseInstanceOf(IllegalArgumentException.class);

        verify(objectMapper, times(1)).readValue(payload, BookingEventMessage.class);
        verifyNoInteractions(cacheManager);
        verifyNoInteractions(vehiclesCache, cleanersByVehicleCache, cleanersCache);

    }

    @Test
    void onMessage_givenEventWithNullBookingId_thenThrowsIllegalArgumentException_andDoesNotClearCaches() throws Exception {

        // Given
        String payload = "{\"type\":\"ANY\"}";
        BookingEventMessage event = new BookingEventMessage(
                firstEventType(),
                null,
                UUID.randomUUID(),
                OffsetDateTime.now(),
                OffsetDateTime.now().plusHours(2),
                2,
                List.of(UUID.randomUUID(), UUID.randomUUID()),
                OffsetDateTime.now()
        );

        when(objectMapper.readValue(payload, BookingEventMessage.class)).thenReturn(event);

        // When / Then
        assertThatThrownBy(() -> bookingEventsListener.onMessage(payload))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid booking event payload")
                .hasCauseInstanceOf(IllegalArgumentException.class);

        verify(objectMapper, times(1)).readValue(payload, BookingEventMessage.class);
        verifyNoInteractions(cacheManager);
        verifyNoInteractions(vehiclesCache, cleanersByVehicleCache, cleanersCache);

    }

    private BookingEventMessage validEvent() {
        return new BookingEventMessage(
                firstEventType(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                OffsetDateTime.now(),
                OffsetDateTime.now().plusHours(2),
                2,
                List.of(UUID.randomUUID(), UUID.randomUUID()),
                OffsetDateTime.now()
        );
    }

    private BookingEventType firstEventType() {
        return BookingEventType.values()[0];
    }

}