package com.booking.professionalservice.service.impl;

import com.booking.common.model.dto.request.CustomPagingRequest;
import com.booking.common.model.pagination.CustomPage;
import com.booking.professionalservice.base.AbstractBaseServiceTest;
import com.booking.professionalservice.model.dto.request.CleanerDto;
import com.booking.professionalservice.model.dto.request.VehicleDto;
import com.booking.professionalservice.model.entity.CleanerEntity;
import com.booking.professionalservice.model.entity.VehicleEntity;
import com.booking.professionalservice.repository.CleanerRepository;
import com.booking.professionalservice.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ProfessionalsServiceImplTest extends AbstractBaseServiceTest {

    private VehicleRepository vehicleRepository;
    private CleanerRepository cleanerRepository;

    private ProfessionalsServiceImpl professionalsService;

    @BeforeEach
    void setUp() {
        this.vehicleRepository = mock(VehicleRepository.class);
        this.cleanerRepository = mock(CleanerRepository.class);
        this.professionalsService = new ProfessionalsServiceImpl(vehicleRepository, cleanerRepository);
    }

    // -------------------------
    // listVehicles
    // -------------------------

    @Test
    void listVehicles_givenNullPagingRequest_andNoVehicles_thenReturnsEmptyPage_andDoesNotQueryCleaners() {

        // Given
        Pageable expectedDefaultPageable = PageRequest.of(0, 20);
        Page<VehicleEntity> emptyPage = new PageImpl<>(List.of(), expectedDefaultPageable, 0);

        when(vehicleRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

        // When
        CustomPage<VehicleDto> result = professionalsService.listVehicles(null);

        // Verify
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(vehicleRepository).findAll(captor.capture());
        verify(cleanerRepository, never()).findByVehicle_IdIn(any());

        Pageable actual = captor.getValue();
        assertThat(actual.getPageNumber()).isEqualTo(0);
        assertThat(actual.getPageSize()).isEqualTo(20);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getPageNumber()).isEqualTo(1);
        assertThat(result.getPageSize()).isEqualTo(20);
        assertThat(result.getTotalElementCount()).isEqualTo(0L);
        assertThat(result.getTotalPageCount()).isEqualTo(0);
    }

    @Test
    void listVehicles_givenPagingRequest_andVehicles_thenQueriesCleaners_andReturnsMappedCustomPage() {

        // Given
        CustomPagingRequest pagingRequest = mock(CustomPagingRequest.class);
        Pageable customPageable = PageRequest.of(1, 2);

        VehicleEntity v1 = vehicleEntity("veh-1", "V-1", "LP-1");
        VehicleEntity v2 = vehicleEntity("veh-2", "V-2", "LP-2");

        CleanerEntity c1 = cleanerEntity("cl-1", "Cleaner 1", v1);
        CleanerEntity c2 = cleanerEntity("cl-2", "Cleaner 2", v1);
        CleanerEntity c3 = cleanerEntity("cl-3", "Cleaner 3", v2);

        Page<VehicleEntity> vehiclePage = new PageImpl<>(List.of(v1, v2), customPageable, 5);

        when(pagingRequest.toPageable()).thenReturn(customPageable);
        when(vehicleRepository.findAll(customPageable)).thenReturn(vehiclePage);
        when(cleanerRepository.findByVehicle_IdIn(any())).thenReturn(List.of(c1, c2, c3));

        // When
        CustomPage<VehicleDto> result = professionalsService.listVehicles(pagingRequest);

        // Verify
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> vehicleIdsCaptor = ArgumentCaptor.forClass((Class) List.class);

        verify(pagingRequest, times(1)).toPageable();
        verify(vehicleRepository, times(1)).findAll(customPageable);
        verify(cleanerRepository, times(1)).findByVehicle_IdIn(vehicleIdsCaptor.capture());

        List<String> capturedVehicleIds = vehicleIdsCaptor.getValue();
        assertThat(capturedVehicleIds).containsExactly("veh-1", "veh-2");

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getPageNumber()).isEqualTo(2);
        assertThat(result.getPageSize()).isEqualTo(2);
        assertThat(result.getTotalElementCount()).isEqualTo(5L);
        assertThat(result.getTotalPageCount()).isEqualTo(3);
    }

    // -------------------------
    // listAllCleaners
    // -------------------------

    @Test
    void listAllCleaners_givenNullPagingRequest_thenUsesDefaultPageable_andReturnsCustomPage() {

        // Given
        Pageable expectedDefaultPageable = PageRequest.of(0, 20);
        CleanerEntity c1 = cleanerEntity("cl-1", "Cleaner 1", null);
        CleanerEntity c2 = cleanerEntity("cl-2", "Cleaner 2", null);
        Page<CleanerEntity> cleanerPage = new PageImpl<>(List.of(c1, c2), expectedDefaultPageable, 2);

        when(cleanerRepository.findAll(any(Pageable.class))).thenReturn(cleanerPage);

        // When
        CustomPage<CleanerDto> result = professionalsService.listAllCleaners(null);

        // Verify
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(cleanerRepository, times(1)).findAll(captor.capture());

        Pageable actual = captor.getValue();
        assertThat(actual.getPageNumber()).isEqualTo(0);
        assertThat(actual.getPageSize()).isEqualTo(20);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getPageNumber()).isEqualTo(1);
        assertThat(result.getPageSize()).isEqualTo(20);
        assertThat(result.getTotalElementCount()).isEqualTo(2L);
        assertThat(result.getTotalPageCount()).isEqualTo(1);
    }

    @Test
    void listAllCleaners_givenPagingRequest_thenUsesCustomPageable_andReturnsCustomPage() {

        // Given
        CustomPagingRequest pagingRequest = mock(CustomPagingRequest.class);
        Pageable customPageable = PageRequest.of(2, 3);
        CleanerEntity c1 = cleanerEntity("cl-1", "Cleaner 1", null);
        Page<CleanerEntity> cleanerPage = new PageImpl<>(List.of(c1), customPageable, 10);

        when(pagingRequest.toPageable()).thenReturn(customPageable);
        when(cleanerRepository.findAll(customPageable)).thenReturn(cleanerPage);

        // When
        CustomPage<CleanerDto> result = professionalsService.listAllCleaners(pagingRequest);

        // Verify
        verify(pagingRequest, times(1)).toPageable();
        verify(cleanerRepository, times(1)).findAll(customPageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getPageNumber()).isEqualTo(3);
        assertThat(result.getPageSize()).isEqualTo(3);
        assertThat(result.getTotalElementCount()).isEqualTo(10L);
        assertThat(result.getTotalPageCount()).isEqualTo(4);
    }

    // -------------------------
    // listCleanersByVehicle
    // -------------------------

    @Test
    void listCleanersByVehicle_givenNullPagingRequest_thenUsesDefaultPageable_andQueriesByVehicleId() {

        // Given
        String vehicleId = "veh-1";
        Pageable expectedDefaultPageable = PageRequest.of(0, 20);
        CleanerEntity c1 = cleanerEntity("cl-1", "Cleaner 1", vehicleEntity(vehicleId, "V-1", "LP-1"));
        Page<CleanerEntity> cleanerPage = new PageImpl<>(List.of(c1), expectedDefaultPageable, 1);

        when(cleanerRepository.findByVehicle_Id(eq(vehicleId), any(Pageable.class))).thenReturn(cleanerPage);

        // When
        CustomPage<CleanerDto> result = professionalsService.listCleanersByVehicle(vehicleId, null);

        // Verify
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(cleanerRepository, times(1)).findByVehicle_Id(eq(vehicleId), captor.capture());

        Pageable actual = captor.getValue();
        assertThat(actual.getPageNumber()).isEqualTo(0);
        assertThat(actual.getPageSize()).isEqualTo(20);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getPageNumber()).isEqualTo(1);
        assertThat(result.getPageSize()).isEqualTo(20);
        assertThat(result.getTotalElementCount()).isEqualTo(1L);
        assertThat(result.getTotalPageCount()).isEqualTo(1);
    }

    @Test
    void listCleanersByVehicle_givenPagingRequest_thenUsesCustomPageable_andQueriesByVehicleId() {

        // Given
        String vehicleId = "veh-1";
        CustomPagingRequest pagingRequest = mock(CustomPagingRequest.class);

        // page index = 2 => 3rd page, size = 5, offset = 10
        Pageable customPageable = PageRequest.of(2, 5);

        CleanerEntity c1 = cleanerEntity("cl-1", "Cleaner 1", vehicleEntity(vehicleId, "V-1", "LP-1"));
        CleanerEntity c2 = cleanerEntity("cl-2", "Cleaner 2", vehicleEntity(vehicleId, "V-1", "LP-1"));

        // total = 12 is now consistent with page index 2 and 2 elements on page
        Page<CleanerEntity> cleanerPage = new PageImpl<>(List.of(c1, c2), customPageable, 12);

        when(pagingRequest.toPageable()).thenReturn(customPageable);
        when(cleanerRepository.findByVehicle_Id(vehicleId, customPageable)).thenReturn(cleanerPage);

        // When
        CustomPage<CleanerDto> result = professionalsService.listCleanersByVehicle(vehicleId, pagingRequest);

        // Verify
        verify(pagingRequest, times(1)).toPageable();
        verify(cleanerRepository, times(1)).findByVehicle_Id(vehicleId, customPageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);

        // page.getNumber() = 2 => pageNumber should be 3
        assertThat(result.getPageNumber()).isEqualTo(3);
        assertThat(result.getPageSize()).isEqualTo(5);
        assertThat(result.getTotalElementCount()).isEqualTo(12L);
        assertThat(result.getTotalPageCount()).isEqualTo(3);
    }

    // -------------------------
    // Helpers
    // -------------------------

    private static VehicleEntity vehicleEntity(String id, String code, String licensePlate) {
        VehicleEntity v = new VehicleEntity();
        v.setId(id);
        v.setCode(code);
        v.setLicensePlate(licensePlate);
        return v;
    }

    private static CleanerEntity cleanerEntity(String id, String fullName, VehicleEntity vehicle) {
        CleanerEntity c = new CleanerEntity();
        c.setId(id);
        c.setFullName(fullName);

        if (vehicle != null) {
            c.setVehicle(vehicle);
        }

        return c;
    }
}