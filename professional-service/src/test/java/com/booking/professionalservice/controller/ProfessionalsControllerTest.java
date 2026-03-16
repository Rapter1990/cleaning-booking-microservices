package com.booking.professionalservice.controller;

import com.booking.common.model.dto.request.CustomPagingRequest;
import com.booking.common.model.pagination.CustomPage;
import com.booking.professionalservice.base.AbstractRestControllerTest;
import com.booking.professionalservice.model.dto.request.CleanerDto;
import com.booking.professionalservice.model.dto.request.VehicleDto;
import com.booking.professionalservice.service.ProfessionalsService;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProfessionalsControllerTest extends AbstractRestControllerTest {

    @MockitoBean
    private ProfessionalsService professionalsService;

    @Test
    void listVehicles_givenValidPagingRequest_whenPostApiVehicles_thenReturnsOkAndMappedResponse() throws Exception {

        // Given
        String requestBody = buildPagingRequestBody(1, 10, "id", "ASC");

        CustomPage<VehicleDto> page = CustomPage.<VehicleDto>builder()
                .content(List.of())
                .pageNumber(1)
                .pageSize(10)
                .totalElementCount(0L)
                .totalPageCount(0)
                .build();

        when(professionalsService.listVehicles(any(CustomPagingRequest.class))).thenReturn(page);

        // When / Then
        mockMvc.perform(
                        post("/api/vehicles")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.pageNumber").value(1))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.totalElementCount").value(0))
                .andExpect(jsonPath("$.totalPageCount").value(0))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0));

        ArgumentCaptor<CustomPagingRequest> captor = ArgumentCaptor.forClass(CustomPagingRequest.class);
        verify(professionalsService, times(1)).listVehicles(captor.capture());

        CustomPagingRequest captured = captor.getValue();
        assertThat(captured).isNotNull();
        assertThat(captured.getPagination()).isNotNull();

        // getter is zero-based by design
        assertThat(captured.getPagination().getPageNumber()).isEqualTo(0);
        assertThat(captured.getPagination().getPageSize()).isEqualTo(10);
        assertThat(captured.getSorting()).isNotNull();
        assertThat(captured.getSorting().getSortBy()).isEqualTo("id");
        assertThat(captured.getSorting().getSortDirection()).isEqualTo("ASC");
    }

    @Test
    void listVehicles_givenValidPagingRequest_whenPostApiV1Vehicles_thenReturnsOkAndCallsService() throws Exception {

        // Given
        String requestBody = buildPagingRequestBody(1, 10, "id", "ASC");

        CustomPage<VehicleDto> page = CustomPage.<VehicleDto>builder()
                .content(List.of())
                .pageNumber(2)
                .pageSize(5)
                .totalElementCount(12L)
                .totalPageCount(3)
                .build();

        when(professionalsService.listVehicles(any(CustomPagingRequest.class))).thenReturn(page);

        // When / Then
        mockMvc.perform(
                        post("/api/v1/vehicles")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.pageNumber").value(2))
                .andExpect(jsonPath("$.pageSize").value(5))
                .andExpect(jsonPath("$.totalElementCount").value(12))
                .andExpect(jsonPath("$.totalPageCount").value(3))
                .andExpect(jsonPath("$.content").isArray());

        verify(professionalsService, times(1)).listVehicles(any(CustomPagingRequest.class));
    }

    @Test
    void listCleanersByVehicle_givenValidPagingRequest_whenPostApiVehicleCleaners_thenReturnsOkAndMappedResponse() throws Exception {

        // Given
        String vehicleId = "11111111-1111-1111-1111-111111111111";
        String requestBody = buildPagingRequestBody(1, 10, "id", "ASC");

        CustomPage<CleanerDto> page = CustomPage.<CleanerDto>builder()
                .content(List.of())
                .pageNumber(1)
                .pageSize(10)
                .totalElementCount(0L)
                .totalPageCount(0)
                .build();

        when(professionalsService.listCleanersByVehicle(eq(vehicleId), any(CustomPagingRequest.class)))
                .thenReturn(page);

        // When / Then
        mockMvc.perform(
                        post("/api/vehicles/{vehicleId}/cleaners", vehicleId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.pageNumber").value(1))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.totalElementCount").value(0))
                .andExpect(jsonPath("$.totalPageCount").value(0))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0));

        ArgumentCaptor<CustomPagingRequest> captor = ArgumentCaptor.forClass(CustomPagingRequest.class);
        verify(professionalsService, times(1)).listCleanersByVehicle(eq(vehicleId), captor.capture());

        CustomPagingRequest captured = captor.getValue();
        assertThat(captured).isNotNull();
        assertThat(captured.getPagination()).isNotNull();
        assertThat(captured.getPagination().getPageNumber()).isEqualTo(0);
        assertThat(captured.getPagination().getPageSize()).isEqualTo(10);
        assertThat(captured.getSorting()).isNotNull();
        assertThat(captured.getSorting().getSortBy()).isEqualTo("id");
        assertThat(captured.getSorting().getSortDirection()).isEqualTo("ASC");
    }

    @Test
    void listCleanersByVehicle_givenValidPagingRequest_whenPostApiV1VehicleCleaners_thenReturnsOkAndCallsService() throws Exception {

        // Given
        String vehicleId = "11111111-1111-1111-1111-111111111111";
        String requestBody = buildPagingRequestBody(1, 10, "id", "ASC");

        CustomPage<CleanerDto> page = CustomPage.<CleanerDto>builder()
                .content(List.of())
                .pageNumber(3)
                .pageSize(5)
                .totalElementCount(11L)
                .totalPageCount(3)
                .build();

        when(professionalsService.listCleanersByVehicle(eq(vehicleId), any(CustomPagingRequest.class)))
                .thenReturn(page);

        // When / Then
        mockMvc.perform(
                        post("/api/v1/vehicles/{vehicleId}/cleaners", vehicleId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.pageNumber").value(3))
                .andExpect(jsonPath("$.pageSize").value(5))
                .andExpect(jsonPath("$.totalElementCount").value(11))
                .andExpect(jsonPath("$.totalPageCount").value(3))
                .andExpect(jsonPath("$.content").isArray());

        verify(professionalsService, times(1))
                .listCleanersByVehicle(eq(vehicleId), any(CustomPagingRequest.class));
    }

    @Test
    void listAllCleaners_givenValidPagingRequest_whenPostApiCleaners_thenReturnsOkAndMappedResponse() throws Exception {

        // Given
        String requestBody = buildPagingRequestBody(1, 10, "id", "ASC");

        CustomPage<CleanerDto> page = CustomPage.<CleanerDto>builder()
                .content(List.of())
                .pageNumber(1)
                .pageSize(10)
                .totalElementCount(0L)
                .totalPageCount(0)
                .build();

        when(professionalsService.listAllCleaners(any(CustomPagingRequest.class))).thenReturn(page);

        // When / Then
        mockMvc.perform(
                        post("/api/cleaners")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.pageNumber").value(1))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.totalElementCount").value(0))
                .andExpect(jsonPath("$.totalPageCount").value(0))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0));

        ArgumentCaptor<CustomPagingRequest> captor = ArgumentCaptor.forClass(CustomPagingRequest.class);
        verify(professionalsService, times(1)).listAllCleaners(captor.capture());

        CustomPagingRequest captured = captor.getValue();
        assertThat(captured).isNotNull();
        assertThat(captured.getPagination()).isNotNull();
        assertThat(captured.getPagination().getPageNumber()).isEqualTo(0);
        assertThat(captured.getPagination().getPageSize()).isEqualTo(10);
        assertThat(captured.getSorting()).isNotNull();
        assertThat(captured.getSorting().getSortBy()).isEqualTo("id");
        assertThat(captured.getSorting().getSortDirection()).isEqualTo("ASC");
    }

    @Test
    void listAllCleaners_givenValidPagingRequest_whenPostApiV1Cleaners_thenReturnsOkAndCallsService() throws Exception {

        // Given
        String requestBody = buildPagingRequestBody(1, 10, "id", "ASC");

        CustomPage<CleanerDto> page = CustomPage.<CleanerDto>builder()
                .content(List.of())
                .pageNumber(4)
                .pageSize(5)
                .totalElementCount(18L)
                .totalPageCount(4)
                .build();

        when(professionalsService.listAllCleaners(any(CustomPagingRequest.class))).thenReturn(page);

        // When / Then
        mockMvc.perform(
                        post("/api/v1/cleaners")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.pageNumber").value(4))
                .andExpect(jsonPath("$.pageSize").value(5))
                .andExpect(jsonPath("$.totalElementCount").value(18))
                .andExpect(jsonPath("$.totalPageCount").value(4))
                .andExpect(jsonPath("$.content").isArray());

        verify(professionalsService, times(1)).listAllCleaners(any(CustomPagingRequest.class));
    }

    @Test
    void listVehicles_givenMissingRequestBody_whenPostApiVehicles_thenReturnsBadRequest() throws Exception {

        // When / Then
        mockMvc.perform(
                        post("/api/vehicles")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isBadRequest());

        verify(professionalsService, never()).listVehicles(any(CustomPagingRequest.class));
    }

    private String buildPagingRequestBody(
            int pageNumber,
            int pageSize,
            String sortBy,
            String sortDirection
    ) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode pagination = root.putObject("pagination");
        pagination.put("pageNumber", pageNumber);
        pagination.put("pageSize", pageSize);

        ObjectNode sorting = root.putObject("sorting");
        sorting.put("sortBy", sortBy);
        sorting.put("sortDirection", sortDirection);

        return objectMapper.writeValueAsString(root);
    }
}