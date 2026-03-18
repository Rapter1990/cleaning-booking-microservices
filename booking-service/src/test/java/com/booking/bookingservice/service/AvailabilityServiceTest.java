package com.booking.bookingservice.service;

import com.booking.bookingservice.base.AbstractBaseServiceTest;
import com.booking.bookingservice.integration.professionals.dto.VehicleDto;
import com.booking.bookingservice.model.entity.BookingEntity;
import com.booking.bookingservice.model.enums.BookingStatus;
import com.booking.bookingservice.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AvailabilityServiceTest extends AbstractBaseServiceTest {

    @Mock
    private ProfessionalsGateway professionalsGateway;

    @Mock
    private BookingRepository bookingRepository;

    private AvailabilityService availabilityService;

    @BeforeEach
    void setUp() {
        availabilityService = new AvailabilityService(professionalsGateway, bookingRepository);
    }

    @Test
    void vehicles_givenGatewayReturnsVehicles_thenReturnsVehicles() {

        // Given
        VehicleDto vehicle1 = mockVehicle("veh-1");
        VehicleDto vehicle2 = mockVehicle("veh-2");

        when(professionalsGateway.listVehicles()).thenReturn(List.of(vehicle1, vehicle2));

        // When
        List<VehicleDto> result = availabilityService.vehicles();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(vehicle1, vehicle2);

        verify(professionalsGateway, times(1)).listVehicles();
        verifyNoInteractions(bookingRepository);
    }

    @Test
    void availabilityByDate_givenNoVehicles_thenReturnsEmptyMap() {

        // Given
        LocalDate date = LocalDate.of(2026, 3, 16); // Monday
        when(professionalsGateway.listVehicles()).thenReturn(List.of());

        // When
        Map<String, Map<String, Map<Integer, List<LocalTime>>>> result =
                availabilityService.availabilityByDate(date);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(professionalsGateway, times(1)).listVehicles();
        verify(professionalsGateway, never()).listCleanerIdsByVehicle(anyString());
        verifyNoInteractions(bookingRepository);
    }

    @Test
    void availabilityByDate_givenVehicleAndCleaners_thenReturnsAvailabilityByVehicleAndCleaner() {

        // Given
        LocalDate date = LocalDate.of(2026, 3, 16); // Monday
        LocalDateTime dayFrom = date.atStartOfDay();
        LocalDateTime dayTo = date.plusDays(1).atStartOfDay();

        VehicleDto vehicle = mockVehicle("veh-1");

        when(professionalsGateway.listVehicles()).thenReturn(List.of(vehicle));
        when(professionalsGateway.listCleanerIdsByVehicle("veh-1"))
                .thenReturn(List.of("cl-1", "cl-2"));

        when(bookingRepository.findActiveBookingsForCleanerWithin("cl-1", BookingStatus.ACTIVE, dayFrom, dayTo))
                .thenReturn(List.of());
        when(bookingRepository.findActiveBookingsForCleanerWithin("cl-2", BookingStatus.ACTIVE, dayFrom, dayTo))
                .thenReturn(List.of());

        // When
        Map<String, Map<String, Map<Integer, List<LocalTime>>>> result =
                availabilityService.availabilityByDate(date);

        // Then
        assertThat(result).containsKey("veh-1");

        Map<String, Map<Integer, List<LocalTime>>> byCleaner = result.get("veh-1");
        assertThat(byCleaner).containsKeys("cl-1", "cl-2");

        assertThat(byCleaner.get("cl-1")).containsKeys(2, 4);
        assertThat(byCleaner.get("cl-2")).containsKeys(2, 4);

        assertThat(byCleaner.get("cl-1").get(2)).isNotEmpty();
        assertThat(byCleaner.get("cl-1").get(4)).isNotEmpty();

        assertThat(byCleaner.get("cl-1").get(2)).contains(LocalTime.of(8, 0));
        assertThat(byCleaner.get("cl-1").get(2)).contains(LocalTime.of(20, 0));

        assertThat(byCleaner.get("cl-1").get(4)).contains(LocalTime.of(8, 0));
        assertThat(byCleaner.get("cl-1").get(4)).contains(LocalTime.of(18, 0));

        verify(professionalsGateway, times(1)).listVehicles();
        verify(professionalsGateway, times(1)).listCleanerIdsByVehicle("veh-1");

        verify(bookingRepository, times(1))
                .findActiveBookingsForCleanerWithin("cl-1", BookingStatus.ACTIVE, dayFrom, dayTo);
        verify(bookingRepository, times(1))
                .findActiveBookingsForCleanerWithin("cl-2", BookingStatus.ACTIVE, dayFrom, dayTo);
    }

    @Test
    void availableCleanersFor_givenUnknownVehicleId_thenThrowsIllegalArgumentException() {

        // Given
        LocalDateTime startAt = LocalDateTime.of(2026, 3, 16, 10, 0);
        VehicleDto vehicle = mockVehicle("veh-1");

        when(professionalsGateway.listVehicles()).thenReturn(List.of(vehicle));

        // When / Then
        assertThatThrownBy(() -> availabilityService.availableCleanersFor("veh-x", startAt, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown vehicleId: veh-x");

        verify(professionalsGateway, times(1)).listVehicles();
        verify(professionalsGateway, never()).listCleanerIdsByVehicle(anyString());
        verifyNoInteractions(bookingRepository);
    }

    @Test
    void availableCleanersFor_givenValidVehicleAndNoConflicts_thenReturnsAllAvailableCleaners() {

        // Given
        LocalDateTime startAt = LocalDateTime.of(2026, 3, 16, 10, 0);
        LocalDateTime from = startAt.toLocalDate().atStartOfDay();
        LocalDateTime to = startAt.toLocalDate().plusDays(1).atStartOfDay();

        VehicleDto vehicle = mockVehicle("veh-1");

        // When
        when(professionalsGateway.listVehicles()).thenReturn(List.of(vehicle));
        when(professionalsGateway.listCleanerIdsByVehicle("veh-1"))
                .thenReturn(List.of("cl-1", "cl-2"));

        when(bookingRepository.findActiveBookingsForCleanerWithin("cl-1", BookingStatus.ACTIVE, from, to))
                .thenReturn(List.of());
        when(bookingRepository.findActiveBookingsForCleanerWithin("cl-2", BookingStatus.ACTIVE, from, to))
                .thenReturn(List.of());

        // Then
        List<String> result = availabilityService.availableCleanersFor("veh-1", startAt, 2);


        assertThat(result).containsExactly("cl-1", "cl-2");

        // Verify
        verify(professionalsGateway, times(1)).listVehicles();
        verify(professionalsGateway, times(1)).listCleanerIdsByVehicle("veh-1");

        verify(bookingRepository, times(1))
                .findActiveBookingsForCleanerWithin("cl-1", BookingStatus.ACTIVE, from, to);
        verify(bookingRepository, times(1))
                .findActiveBookingsForCleanerWithin("cl-2", BookingStatus.ACTIVE, from, to);

        verify(bookingRepository, never())
                .findActiveBookingsForCleanerWithinExcludingBooking(anyString(), any(), any(), any(), anyString());
    }

    @Test
    void availableCleanersFor_givenExcludeBookingIdAndOneConflictingCleaner_thenReturnsOnlyAvailableCleaners() {

        // Given
        LocalDateTime startAt = LocalDateTime.of(2026, 3, 16, 10, 0);
        LocalDateTime from = startAt.toLocalDate().atStartOfDay();
        LocalDateTime to = startAt.toLocalDate().plusDays(1).atStartOfDay();

        VehicleDto vehicle = mockVehicle("veh-1");
        BookingEntity conflictingBooking = mockBooking(
                LocalDateTime.of(2026, 3, 16, 9, 0),
                LocalDateTime.of(2026, 3, 16, 11, 0)
        );

        // When
        when(professionalsGateway.listVehicles()).thenReturn(List.of(vehicle));
        when(professionalsGateway.listCleanerIdsByVehicle("veh-1"))
                .thenReturn(List.of("cl-1", "cl-2"));

        when(bookingRepository.findActiveBookingsForCleanerWithinExcludingBooking(
                "cl-1", BookingStatus.ACTIVE, from, to, "booking-1"))
                .thenReturn(List.of(conflictingBooking));

        when(bookingRepository.findActiveBookingsForCleanerWithinExcludingBooking(
                "cl-2", BookingStatus.ACTIVE, from, to, "booking-1"))
                .thenReturn(List.of());

        // Then
        List<String> result = availabilityService.availableCleanersFor(
                "veh-1",
                startAt,
                2,
                "booking-1"
        );

        assertThat(result).containsExactly("cl-2");

        // Verify
        verify(professionalsGateway, times(1)).listVehicles();
        verify(professionalsGateway, times(1)).listCleanerIdsByVehicle("veh-1");

        verify(bookingRepository, times(1))
                .findActiveBookingsForCleanerWithinExcludingBooking("cl-1", BookingStatus.ACTIVE, from, to, "booking-1");
        verify(bookingRepository, times(1))
                .findActiveBookingsForCleanerWithinExcludingBooking("cl-2", BookingStatus.ACTIVE, from, to, "booking-1");

        verify(bookingRepository, never())
                .findActiveBookingsForCleanerWithin(anyString(), any(), any(), any());

    }

    @Test
    void findVehicleWithCapacity_givenEnoughCapacityOnFirstVehicle_thenReturnsCandidate() {

        // Given
        LocalDateTime startAt = LocalDateTime.of(2026, 3, 16, 10, 0);
        LocalDateTime from = startAt.toLocalDate().atStartOfDay();
        LocalDateTime to = startAt.toLocalDate().plusDays(1).atStartOfDay();

        VehicleDto vehicle1 = mockVehicle("veh-1");
        VehicleDto vehicle2 = mockVehicle("veh-2");

        // When
        when(professionalsGateway.listVehicles()).thenReturn(List.of(vehicle1, vehicle2));
        when(professionalsGateway.listCleanerIdsByVehicle("veh-1"))
                .thenReturn(List.of("cl-1", "cl-2"));

        when(bookingRepository.findActiveBookingsForCleanerWithin("cl-1", BookingStatus.ACTIVE, from, to))
                .thenReturn(List.of());
        when(bookingRepository.findActiveBookingsForCleanerWithin("cl-2", BookingStatus.ACTIVE, from, to))
                .thenReturn(List.of());

        // Then
        Optional<AvailabilityService.VehicleCandidate> result =
                availabilityService.findVehicleWithCapacity(startAt, 2, 2);

        assertThat(result).isPresent();
        assertThat(result.get().vehicleId()).isEqualTo("veh-1");
        assertThat(result.get().availableCleanerIds()).containsExactly("cl-1", "cl-2");

        // Verify
        verify(professionalsGateway, atLeastOnce()).listVehicles();
        verify(professionalsGateway).listCleanerIdsByVehicle("veh-1");
        verify(professionalsGateway, never()).listCleanerIdsByVehicle("veh-2");

    }

    @Test
    void findVehicleWithCapacity_givenNoVehicleHasEnoughCapacity_thenReturnsEmpty() {

        // Given
        LocalDateTime startAt = LocalDateTime.of(2026, 3, 16, 10, 0);
        LocalDateTime from = startAt.toLocalDate().atStartOfDay();
        LocalDateTime to = startAt.toLocalDate().plusDays(1).atStartOfDay();

        VehicleDto vehicle1 = mockVehicle("veh-1");

        // When
        when(professionalsGateway.listVehicles()).thenReturn(List.of(vehicle1));
        when(professionalsGateway.listCleanerIdsByVehicle("veh-1"))
                .thenReturn(List.of("cl-1"));

        when(bookingRepository.findActiveBookingsForCleanerWithinExcludingBooking(
                "cl-1", BookingStatus.ACTIVE, from, to, "booking-99"))
                .thenReturn(List.of());

        // Then
        Optional<AvailabilityService.VehicleCandidate> result =
                availabilityService.findVehicleWithCapacity(startAt, 2, 2, "booking-99");


        assertThat(result).isEmpty();

        // Verify
        verify(professionalsGateway, atLeastOnce()).listVehicles();
        verify(professionalsGateway).listCleanerIdsByVehicle("veh-1");
        verify(bookingRepository, times(1))
                .findActiveBookingsForCleanerWithinExcludingBooking("cl-1", BookingStatus.ACTIVE, from, to, "booking-99");

    }

    private VehicleDto mockVehicle(String id) {
        VehicleDto vehicle = mock(VehicleDto.class);
        when(vehicle.id()).thenReturn(id);
        return vehicle;
    }

    private BookingEntity mockBooking(LocalDateTime startAt, LocalDateTime endAt) {
        BookingEntity booking = mock(BookingEntity.class);
        when(booking.getStartAt()).thenReturn(startAt);
        when(booking.getEndAt()).thenReturn(endAt);
        return booking;
    }

}