package com.booking.bookingservice.service;

import com.booking.bookingservice.base.AbstractBaseServiceTest;
import com.booking.bookingservice.exception.DomainException;
import com.booking.bookingservice.integration.kafka.BookingEventMessage;
import com.booking.bookingservice.integration.kafka.BookingEventPublisher;
import com.booking.bookingservice.integration.kafka.BookingEventType;
import com.booking.bookingservice.model.entity.BookingCleanerEntity;
import com.booking.bookingservice.model.entity.BookingEntity;
import com.booking.bookingservice.model.enums.BookingStatus;
import com.booking.bookingservice.repository.BookingCleanerRepository;
import com.booking.bookingservice.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BookingAppServiceTest extends AbstractBaseServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingCleanerRepository bookingCleanerRepository;

    @Mock
    private AvailabilityService availabilityService;

    @Mock
    private BookingEventPublisher bookingEventPublisher;

    private BookingAppService bookingAppService;

    @BeforeEach
    void setUp() {
        bookingAppService = new BookingAppService(
                bookingRepository,
                bookingCleanerRepository,
                availabilityService,
                bookingEventPublisher
        );
    }

    @Test
    void create_givenProfessionalCountLessThanOne_thenThrowsDomainException() {

        // Given
        LocalDateTime startAt = LocalDateTime.of(2026, 3, 16, 10, 0);

        // When / Then
        assertThatThrownBy(() -> bookingAppService.create(startAt, 2, 0))
                .isInstanceOf(DomainException.class)
                .hasMessage("Cleaner professional count must be 1, 2, or 3.");

        // Verify
        verifyNoInteractions(availabilityService, bookingRepository, bookingCleanerRepository, bookingEventPublisher);

    }

    @Test
    void create_givenProfessionalCountGreaterThanThree_thenThrowsDomainException() {

        // Given
        LocalDateTime startAt = LocalDateTime.of(2026, 3, 16, 10, 0);

        // When / Then
        assertThatThrownBy(() -> bookingAppService.create(startAt, 2, 4))
                .isInstanceOf(DomainException.class)
                .hasMessage("Cleaner professional count must be 1, 2, or 3.");

        verifyNoInteractions(availabilityService, bookingRepository, bookingCleanerRepository, bookingEventPublisher);
    }

    @Test
    void create_givenNoVehicleCandidate_thenThrowsDomainException() {

        // Given
        LocalDateTime startAt = LocalDateTime.of(2026, 3, 16, 10, 0);

        when(availabilityService.findVehicleWithCapacity(startAt, 2, 2))
                .thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> bookingAppService.create(startAt, 2, 2))
                .isInstanceOf(DomainException.class)
                .hasMessage("No available vehicle/cleaners for the requested time window.");

        verify(availabilityService, times(1)).findVehicleWithCapacity(startAt, 2, 2);
        verifyNoInteractions(bookingRepository, bookingCleanerRepository, bookingEventPublisher);
    }

    @Test
    void create_givenValidRequest_thenSavesBookingAssignmentsAndPublishesEvent() {

        // Given
        LocalDateTime startAt = LocalDateTime.of(2026, 3, 16, 10, 0);
        LocalDateTime endAt = startAt.plusHours(2);

        AvailabilityService.VehicleCandidate candidate =
                new AvailabilityService.VehicleCandidate("veh-1", List.of("cl-1", "cl-2", "cl-3"));

        when(availabilityService.findVehicleWithCapacity(startAt, 2, 2))
                .thenReturn(Optional.of(candidate));

        when(bookingRepository.save(any(BookingEntity.class))).thenAnswer(invocation -> {
            BookingEntity booking = invocation.getArgument(0);
            ReflectionTestUtils.setField(booking, "id", "booking-1");
            return booking;
        });

        // When
        BookingEntity result = bookingAppService.create(startAt, 2, 2);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("booking-1");
        assertThat(result.getStartAt()).isEqualTo(startAt);
        assertThat(result.getEndAt()).isEqualTo(endAt);
        assertThat(result.getDurationHours()).isEqualTo(2);
        assertThat(result.getVehicleId()).isEqualTo("veh-1");
        assertThat(result.getStatus()).isEqualTo(BookingStatus.ACTIVE);

        ArgumentCaptor<BookingEntity> bookingCaptor = ArgumentCaptor.forClass(BookingEntity.class);
        verify(bookingRepository, times(1)).save(bookingCaptor.capture());

        BookingEntity savedBooking = bookingCaptor.getValue();
        assertThat(savedBooking.getStartAt()).isEqualTo(startAt);
        assertThat(savedBooking.getEndAt()).isEqualTo(endAt);
        assertThat(savedBooking.getDurationHours()).isEqualTo(2);
        assertThat(savedBooking.getVehicleId()).isEqualTo("veh-1");
        assertThat(savedBooking.getStatus()).isEqualTo(BookingStatus.ACTIVE);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BookingCleanerEntity>> cleanerAssignmentsCaptor =
                ArgumentCaptor.forClass((Class) List.class);

        verify(bookingCleanerRepository, times(1)).saveAll(cleanerAssignmentsCaptor.capture());

        List<BookingCleanerEntity> savedAssignments = cleanerAssignmentsCaptor.getValue();
        assertThat(savedAssignments).hasSize(2);
        assertThat(savedAssignments)
                .extracting(BookingCleanerEntity::getCleanerId)
                .containsExactly("cl-1", "cl-2");

        ArgumentCaptor<BookingEventMessage> eventCaptor = ArgumentCaptor.forClass(BookingEventMessage.class);
        verify(bookingEventPublisher, times(1)).publish(eventCaptor.capture());

        BookingEventMessage publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.type()).isEqualTo(BookingEventType.BOOKING_CREATED);
        assertThat(publishedEvent.bookingId()).isEqualTo("booking-1");
        assertThat(publishedEvent.vehicleId()).isEqualTo("veh-1");
        assertThat(publishedEvent.startAt()).isEqualTo(OffsetDateTime.of(startAt, ZoneOffset.UTC));
        assertThat(publishedEvent.endAt()).isEqualTo(OffsetDateTime.of(endAt, ZoneOffset.UTC));
        assertThat(publishedEvent.durationHours()).isEqualTo(2);
        assertThat(publishedEvent.cleanerIds()).containsExactly("cl-1", "cl-2");
        assertThat(publishedEvent.occurredAt()).isNotNull();

        verify(availabilityService, times(1)).findVehicleWithCapacity(startAt, 2, 2);
    }

    @Test
    void create_givenDuplicateCleanerIdsAndInsufficientDistinctCleaners_thenThrowsDomainException() {

        // Given
        LocalDateTime startAt = LocalDateTime.of(2026, 3, 16, 10, 0);

        AvailabilityService.VehicleCandidate candidate =
                new AvailabilityService.VehicleCandidate("veh-1", List.of("cl-1", "cl-1"));

        when(availabilityService.findVehicleWithCapacity(startAt, 2, 2))
                .thenReturn(Optional.of(candidate));

        when(bookingRepository.save(any(BookingEntity.class))).thenAnswer(invocation -> {
            BookingEntity booking = invocation.getArgument(0);
            ReflectionTestUtils.setField(booking, "id", "booking-1");
            return booking;
        });

        // When / Then
        assertThatThrownBy(() -> bookingAppService.create(startAt, 2, 2))
                .isInstanceOf(DomainException.class)
                .hasMessage("Not enough available cleaners.");

        verify(availabilityService, times(1)).findVehicleWithCapacity(startAt, 2, 2);
        verify(bookingRepository, times(1)).save(any(BookingEntity.class));
        verify(bookingCleanerRepository, never()).saveAll(any());
        verifyNoInteractions(bookingEventPublisher);
    }

    @Test
    void reschedule_givenBookingNotFound_thenThrowsDomainException() {

        // Given
        String bookingId = "booking-1";
        LocalDateTime newStartAt = LocalDateTime.of(2026, 3, 17, 12, 0);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> bookingAppService.reschedule(bookingId, newStartAt, 4))
                .isInstanceOf(DomainException.class)
                .hasMessage("Booking not found: booking-1");

        verify(bookingRepository, times(1)).findById(bookingId);
        verifyNoMoreInteractions(bookingRepository);
        verifyNoInteractions(bookingCleanerRepository, availabilityService, bookingEventPublisher);
    }

    @Test
    void reschedule_givenSameVehicleAndSameCleanersStillAvailable_thenKeepsAssignments() {

        // Given
        String bookingId = "booking-1";

        BookingEntity existing = new BookingEntity(
                LocalDateTime.of(2026, 3, 16, 10, 0),
                LocalDateTime.of(2026, 3, 16, 12, 0),
                2,
                "veh-1",
                BookingStatus.ACTIVE
        );
        ReflectionTestUtils.setField(existing, "id", bookingId);

        LocalDateTime newStartAt = LocalDateTime.of(2026, 3, 17, 14, 0);
        LocalDateTime newEndAt = newStartAt.plusHours(4);

        BookingCleanerEntity previous1 = mock(BookingCleanerEntity.class);
        BookingCleanerEntity previous2 = mock(BookingCleanerEntity.class);

        when(previous1.getCleanerId()).thenReturn("cl-1");
        when(previous2.getCleanerId()).thenReturn("cl-2");

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(existing));
        when(bookingCleanerRepository.findByBooking_Id(bookingId)).thenReturn(List.of(previous1, previous2));
        when(availabilityService.availableCleanersFor("veh-1", newStartAt, 4, bookingId))
                .thenReturn(List.of("cl-1", "cl-2", "cl-3"));
        when(bookingRepository.save(existing)).thenReturn(existing);

        // When
        BookingEntity result = bookingAppService.reschedule(bookingId, newStartAt, 4);

        // Then
        assertThat(result).isSameAs(existing);
        assertThat(result.getStartAt()).isEqualTo(newStartAt);
        assertThat(result.getEndAt()).isEqualTo(newEndAt);
        assertThat(result.getDurationHours()).isEqualTo(4);
        assertThat(result.getVehicleId()).isEqualTo("veh-1");

        verify(bookingRepository, times(1)).findById(bookingId);
        verify(bookingCleanerRepository, times(1)).findByBooking_Id(bookingId);
        verify(availabilityService, times(1))
                .availableCleanersFor("veh-1", newStartAt, 4, bookingId);
        verify(availabilityService, never())
                .findVehicleWithCapacity(any(), anyInt(), anyInt(), anyString());

        verify(bookingCleanerRepository, times(1)).deleteAllByBookingId(bookingId);
        verify(bookingRepository, times(1)).save(existing);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BookingCleanerEntity>> cleanerAssignmentsCaptor =
                ArgumentCaptor.forClass((Class) List.class);

        verify(bookingCleanerRepository, times(1)).saveAll(cleanerAssignmentsCaptor.capture());

        List<BookingCleanerEntity> savedAssignments = cleanerAssignmentsCaptor.getValue();
        assertThat(savedAssignments).hasSize(2);
        assertThat(savedAssignments)
                .extracting(BookingCleanerEntity::getCleanerId)
                .containsExactly("cl-1", "cl-2");

        ArgumentCaptor<BookingEventMessage> eventCaptor = ArgumentCaptor.forClass(BookingEventMessage.class);
        verify(bookingEventPublisher, times(1)).publish(eventCaptor.capture());

        BookingEventMessage publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.type()).isEqualTo(BookingEventType.BOOKING_RESCHEDULED);
        assertThat(publishedEvent.bookingId()).isEqualTo(bookingId);
        assertThat(publishedEvent.vehicleId()).isEqualTo("veh-1");
        assertThat(publishedEvent.startAt()).isEqualTo(OffsetDateTime.of(newStartAt, ZoneOffset.UTC));
        assertThat(publishedEvent.endAt()).isEqualTo(OffsetDateTime.of(newEndAt, ZoneOffset.UTC));
        assertThat(publishedEvent.durationHours()).isEqualTo(4);
        assertThat(publishedEvent.cleanerIds()).containsExactly("cl-1", "cl-2");
        assertThat(publishedEvent.occurredAt()).isNotNull();
    }

    @Test
    void reschedule_givenSameVehicleCannotKeepCleaners_thenSelectsAnotherVehicleAndCleaners() {

        // Given
        String bookingId = "booking-1";

        BookingEntity existing = new BookingEntity(
                LocalDateTime.of(2026, 3, 16, 10, 0),
                LocalDateTime.of(2026, 3, 16, 12, 0),
                2,
                "veh-1",
                BookingStatus.ACTIVE
        );
        ReflectionTestUtils.setField(existing, "id", bookingId);

        LocalDateTime newStartAt = LocalDateTime.of(2026, 3, 17, 15, 0);
        LocalDateTime newEndAt = newStartAt.plusHours(2);

        BookingCleanerEntity previous1 = mock(BookingCleanerEntity.class);
        BookingCleanerEntity previous2 = mock(BookingCleanerEntity.class);

        when(previous1.getCleanerId()).thenReturn("cl-1");
        when(previous2.getCleanerId()).thenReturn("cl-2");

        AvailabilityService.VehicleCandidate vehicleCandidate =
                new AvailabilityService.VehicleCandidate("veh-2", List.of("cl-3", "cl-4", "cl-4"));

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(existing));
        when(bookingCleanerRepository.findByBooking_Id(bookingId)).thenReturn(List.of(previous1, previous2));

        when(availabilityService.availableCleanersFor("veh-1", newStartAt, 2, bookingId))
                .thenReturn(List.of("cl-1"));

        when(availabilityService.findVehicleWithCapacity(newStartAt, 2, 2, bookingId))
                .thenReturn(Optional.of(vehicleCandidate));

        when(bookingRepository.save(existing)).thenReturn(existing);

        // When
        BookingEntity result = bookingAppService.reschedule(bookingId, newStartAt, 2);

        // Then
        assertThat(result).isSameAs(existing);
        assertThat(result.getStartAt()).isEqualTo(newStartAt);
        assertThat(result.getEndAt()).isEqualTo(newEndAt);
        assertThat(result.getDurationHours()).isEqualTo(2);
        assertThat(result.getVehicleId()).isEqualTo("veh-2");

        verify(availabilityService, times(1))
                .availableCleanersFor("veh-1", newStartAt, 2, bookingId);
        verify(availabilityService, times(1))
                .findVehicleWithCapacity(newStartAt, 2, 2, bookingId);

        verify(bookingCleanerRepository, times(1)).deleteAllByBookingId(bookingId);
        verify(bookingRepository, times(1)).save(existing);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BookingCleanerEntity>> cleanerAssignmentsCaptor =
                ArgumentCaptor.forClass((Class) List.class);

        verify(bookingCleanerRepository, times(1)).saveAll(cleanerAssignmentsCaptor.capture());

        List<BookingCleanerEntity> savedAssignments = cleanerAssignmentsCaptor.getValue();
        assertThat(savedAssignments).hasSize(2);
        assertThat(savedAssignments)
                .extracting(BookingCleanerEntity::getCleanerId)
                .containsExactly("cl-3", "cl-4");

        ArgumentCaptor<BookingEventMessage> eventCaptor = ArgumentCaptor.forClass(BookingEventMessage.class);
        verify(bookingEventPublisher, times(1)).publish(eventCaptor.capture());

        BookingEventMessage publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.type()).isEqualTo(BookingEventType.BOOKING_RESCHEDULED);
        assertThat(publishedEvent.bookingId()).isEqualTo(bookingId);
        assertThat(publishedEvent.vehicleId()).isEqualTo("veh-2");
        assertThat(publishedEvent.startAt()).isEqualTo(OffsetDateTime.of(newStartAt, ZoneOffset.UTC));
        assertThat(publishedEvent.endAt()).isEqualTo(OffsetDateTime.of(newEndAt, ZoneOffset.UTC));
        assertThat(publishedEvent.durationHours()).isEqualTo(2);
        assertThat(publishedEvent.cleanerIds()).containsExactly("cl-3", "cl-4");
        assertThat(publishedEvent.occurredAt()).isNotNull();
    }

    @Test
    void reschedule_givenNoAvailabilityForAnotherVehicle_thenThrowsDomainException() {

        // Given
        String bookingId = "booking-1";

        BookingEntity existing = new BookingEntity(
                LocalDateTime.of(2026, 3, 16, 10, 0),
                LocalDateTime.of(2026, 3, 16, 12, 0),
                2,
                "veh-1",
                BookingStatus.ACTIVE
        );
        ReflectionTestUtils.setField(existing, "id", bookingId);

        LocalDateTime newStartAt = LocalDateTime.of(2026, 3, 17, 15, 0);

        BookingCleanerEntity previous1 = mock(BookingCleanerEntity.class);
        BookingCleanerEntity previous2 = mock(BookingCleanerEntity.class);

        when(previous1.getCleanerId()).thenReturn("cl-1");
        when(previous2.getCleanerId()).thenReturn("cl-2");

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(existing));
        when(bookingCleanerRepository.findByBooking_Id(bookingId)).thenReturn(List.of(previous1, previous2));

        when(availabilityService.availableCleanersFor("veh-1", newStartAt, 2, bookingId))
                .thenReturn(List.of("cl-1"));

        when(availabilityService.findVehicleWithCapacity(newStartAt, 2, 2, bookingId))
                .thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> bookingAppService.reschedule(bookingId, newStartAt, 2))
                .isInstanceOf(DomainException.class)
                .hasMessage("No availability to reschedule for the requested time window.");

        verify(bookingCleanerRepository, never()).deleteAllByBookingId(anyString());
        verify(bookingRepository, never()).save(existing);
        verify(bookingCleanerRepository, never()).saveAll(any());
        verifyNoInteractions(bookingEventPublisher);
    }

    @Test
    void reschedule_givenAlternativeVehicleButInsufficientDistinctCleaners_thenThrowsDomainException() {

        // Given
        String bookingId = "booking-1";

        BookingEntity existing = new BookingEntity(
                LocalDateTime.of(2026, 3, 16, 10, 0),
                LocalDateTime.of(2026, 3, 16, 12, 0),
                2,
                "veh-1",
                BookingStatus.ACTIVE
        );
        ReflectionTestUtils.setField(existing, "id", bookingId);

        LocalDateTime newStartAt = LocalDateTime.of(2026, 3, 17, 15, 0);

        BookingCleanerEntity previous1 = mock(BookingCleanerEntity.class);
        BookingCleanerEntity previous2 = mock(BookingCleanerEntity.class);

        when(previous1.getCleanerId()).thenReturn("cl-1");
        when(previous2.getCleanerId()).thenReturn("cl-2");

        AvailabilityService.VehicleCandidate vehicleCandidate =
                new AvailabilityService.VehicleCandidate("veh-2", List.of("cl-3", "cl-3"));

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(existing));
        when(bookingCleanerRepository.findByBooking_Id(bookingId)).thenReturn(List.of(previous1, previous2));

        when(availabilityService.availableCleanersFor("veh-1", newStartAt, 2, bookingId))
                .thenReturn(List.of("cl-1"));

        when(availabilityService.findVehicleWithCapacity(newStartAt, 2, 2, bookingId))
                .thenReturn(Optional.of(vehicleCandidate));

        // When / Then
        assertThatThrownBy(() -> bookingAppService.reschedule(bookingId, newStartAt, 2))
                .isInstanceOf(DomainException.class)
                .hasMessage("Not enough available cleaners to reschedule.");

        verify(bookingCleanerRepository, never()).deleteAllByBookingId(anyString());
        verify(bookingRepository, never()).save(existing);
        verify(bookingCleanerRepository, never()).saveAll(any());
        verifyNoInteractions(bookingEventPublisher);
    }

}