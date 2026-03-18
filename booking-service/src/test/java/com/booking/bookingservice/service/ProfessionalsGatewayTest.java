package com.booking.bookingservice.service;

import com.booking.bookingservice.base.AbstractBaseServiceTest;
import com.booking.bookingservice.integration.professionals.ProfessionalsClient;
import com.booking.bookingservice.integration.professionals.dto.CleanerDto;
import com.booking.bookingservice.integration.professionals.dto.VehicleDto;
import com.booking.common.model.dto.request.CustomPagingRequest;
import com.booking.common.model.dto.response.CustomPagingResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProfessionalsGatewayTest extends AbstractBaseServiceTest {

    @Mock
    private ProfessionalsClient professionalsClient;

    private ProfessionalsGateway professionalsGateway;

    @BeforeEach
    void setUp() {
        professionalsGateway = new ProfessionalsGateway(professionalsClient);
    }

    @Test
    void listVehicles_givenClientReturnsVehicles_thenReturnsVehicleListAndUsesDefaultPagingRequest() {

        // Given
        VehicleDto vehicle1 = mock(VehicleDto.class);
        VehicleDto vehicle2 = mock(VehicleDto.class);

        CustomPagingResponse<VehicleDto> response = CustomPagingResponse.<VehicleDto>builder()
                .content(List.of(vehicle1, vehicle2))
                .pageNumber(1)
                .pageSize(100)
                .totalElementCount(2L)
                .totalPageCount(1)
                .build();

        when(professionalsClient.listVehicles(any(CustomPagingRequest.class))).thenReturn(response);

        // When
        List<VehicleDto> result = professionalsGateway.listVehicles();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(vehicle1, vehicle2);

        ArgumentCaptor<CustomPagingRequest> captor =
                ArgumentCaptor.forClass(CustomPagingRequest.class);

        verify(professionalsClient, times(1)).listVehicles(captor.capture());

        CustomPagingRequest capturedRequest = captor.getValue();
        assertThat(capturedRequest).isNotNull();
        assertThat(capturedRequest.getPagination()).isNotNull();
        assertThat(capturedRequest.getSorting()).isNull();

        Object rawPageNumber =
                ReflectionTestUtils.getField(capturedRequest.getPagination(), "pageNumber");
        Object rawPageSize =
                ReflectionTestUtils.getField(capturedRequest.getPagination(), "pageSize");

        assertThat(rawPageNumber).isEqualTo(2);
        assertThat(rawPageSize).isEqualTo(100);

    }

    @Test
    void listVehicles_givenClientReturnsEmptyContent_thenReturnsEmptyList() {

        // Given
        CustomPagingResponse<VehicleDto> response = CustomPagingResponse.<VehicleDto>builder()
                .content(List.of())
                .pageNumber(1)
                .pageSize(100)
                .totalElementCount(0L)
                .totalPageCount(0)
                .build();

        // When
        when(professionalsClient.listVehicles(any(CustomPagingRequest.class))).thenReturn(response);

        // Then
        List<VehicleDto> result = professionalsGateway.listVehicles();

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        // Verify
        verify(professionalsClient, times(1)).listVehicles(any(CustomPagingRequest.class));

    }

    @Test
    void listCleanerIdsByVehicle_givenClientReturnsCleaners_thenReturnsCleanerIdsAndUsesDefaultPagingRequest() {

        // Given
        String vehicleId = "veh-1";

        CleanerDto cleaner1 = mock(CleanerDto.class);
        CleanerDto cleaner2 = mock(CleanerDto.class);

        when(cleaner1.id()).thenReturn("cl-1");
        when(cleaner2.id()).thenReturn("cl-2");

        CustomPagingResponse<CleanerDto> response = CustomPagingResponse.<CleanerDto>builder()
                .content(List.of(cleaner1, cleaner2))
                .pageNumber(1)
                .pageSize(100)
                .totalElementCount(2L)
                .totalPageCount(1)
                .build();

        // When
        when(professionalsClient.listCleanersByVehicle(eq(vehicleId), any(CustomPagingRequest.class)))
                .thenReturn(response);

        // Then
        List<String> result = professionalsGateway.listCleanerIdsByVehicle(vehicleId);

        assertThat(result).isNotNull();
        assertThat(result).containsExactly("cl-1", "cl-2");

        ArgumentCaptor<CustomPagingRequest> captor =
                ArgumentCaptor.forClass(CustomPagingRequest.class);

        verify(professionalsClient, times(1))
                .listCleanersByVehicle(eq(vehicleId), captor.capture());

        CustomPagingRequest capturedRequest = captor.getValue();
        assertThat(capturedRequest).isNotNull();
        assertThat(capturedRequest.getPagination()).isNotNull();
        assertThat(capturedRequest.getSorting()).isNull();

        Object rawPageNumber =
                ReflectionTestUtils.getField(capturedRequest.getPagination(), "pageNumber");
        Object rawPageSize =
                ReflectionTestUtils.getField(capturedRequest.getPagination(), "pageSize");

        assertThat(rawPageNumber).isEqualTo(2);
        assertThat(rawPageSize).isEqualTo(100);

        // Verify
        verify(cleaner1, times(1)).id();
        verify(cleaner2, times(1)).id();

    }

    @Test
    void listCleanerIdsByVehicle_givenClientReturnsEmptyContent_thenReturnsEmptyList() {

        // Given
        String vehicleId = "veh-1";

        CustomPagingResponse<CleanerDto> response = CustomPagingResponse.<CleanerDto>builder()
                .content(List.of())
                .pageNumber(1)
                .pageSize(100)
                .totalElementCount(0L)
                .totalPageCount(0)
                .build();

        // When
        when(professionalsClient.listCleanersByVehicle(eq(vehicleId), any(CustomPagingRequest.class)))
                .thenReturn(response);

        // Then
        List<String> result = professionalsGateway.listCleanerIdsByVehicle(vehicleId);

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        // Verify
        verify(professionalsClient, times(1))
                .listCleanersByVehicle(eq(vehicleId), any(CustomPagingRequest.class));

    }

    @Test
    void listVehiclesFallback_givenThrowable_thenThrowsIllegalStateException() {

        // Given
        RuntimeException cause = new RuntimeException("service down");

        // When / Then
        assertThatThrownBy(() -> professionalsGateway.listVehiclesFallback(cause))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("professionals-service is unavailable")
                .hasCause(cause);

        // Verify
        verifyNoInteractions(professionalsClient);

    }

    @Test
    void listCleanersByVehicleFallback_givenVehicleIdAndThrowable_thenThrowsIllegalStateException() {

        // Given
        String vehicleId = "veh-123";
        RuntimeException cause = new RuntimeException("service down");

        // When / Then
        assertThatThrownBy(() -> professionalsGateway.listCleanersByVehicleFallback(vehicleId, cause))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("professionals-service is unavailable (cleaners for vehicleId=veh-123)")
                .hasCause(cause);

        // Verify
        verifyNoInteractions(professionalsClient);

    }

}