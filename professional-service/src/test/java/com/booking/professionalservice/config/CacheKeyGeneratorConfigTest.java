package com.booking.professionalservice.config;

import com.booking.common.model.dto.request.CustomPagingRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.cache.interceptor.KeyGenerator;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CacheKeyGeneratorConfigTest {

    private KeyGenerator keyGenerator;

    @BeforeEach
    void setUp() {
        CacheKeyGeneratorConfig config = new CacheKeyGeneratorConfig();
        this.keyGenerator = config.customPagingKeyGenerator();
    }

    @Test
    void customPagingKeyGenerator_shouldCreateBean() {
        assertThat(keyGenerator).isNotNull();
    }

    @Test
    void generate_givenNoParams_thenReturnsOnlyMethodName() throws Exception {
        // Given
        Method method = MethodHolder.class.getDeclaredMethod("noArgsMethod");

        // When
        Object result = keyGenerator.generate(this, method);

        // Then
        assertThat(result).isEqualTo("noArgsMethod");
    }

    @Test
    void generate_givenNullParamForListVehicles_thenReturnsNullKeyPart() throws Exception {
        // Given
        Method method = MethodHolder.class.getDeclaredMethod("listVehicles", CustomPagingRequest.class);

        // When
        Object result = keyGenerator.generate(this, method, (Object) null);

        // Then
        assertThat(result).isEqualTo("listVehicles::null");
    }

    @Test
    void generate_givenCustomPagingRequestWithPaginationAndSorting_forListVehicles_thenBuildsExpectedKey() throws Exception {
        // Given
        Method method = MethodHolder.class.getDeclaredMethod("listVehicles", CustomPagingRequest.class);

        CustomPagingRequest pagingRequest = mock(CustomPagingRequest.class, Answers.RETURNS_DEEP_STUBS);

        when(pagingRequest.getPagination().getPageNumber()).thenReturn(2);
        when(pagingRequest.getPagination().getPageSize()).thenReturn(20);
        when(pagingRequest.getSorting().getSortBy()).thenReturn("fullName");
        when(pagingRequest.getSorting().getSortDirection()).thenReturn("ASC");

        // When
        Object result = keyGenerator.generate(this, method, pagingRequest);

        // Then
        assertThat(result)
                .isEqualTo("listVehicles::page=2,size=20,sortBy=fullName,sortDir=ASC");

        verify(pagingRequest, atLeastOnce()).getPagination();
        verify(pagingRequest, atLeastOnce()).getSorting();
    }

    @Test
    void generate_givenCustomPagingRequestWithNullPaginationAndSorting_forListAllCleaners_thenBuildsDefaultKey() throws Exception {
        // Given
        Method method = MethodHolder.class.getDeclaredMethod("listAllCleaners", CustomPagingRequest.class);

        CustomPagingRequest pagingRequest = mock(CustomPagingRequest.class, Answers.RETURNS_DEEP_STUBS);
        when(pagingRequest.getPagination()).thenReturn(null);
        when(pagingRequest.getSorting()).thenReturn(null);

        // When
        Object result = keyGenerator.generate(this, method, pagingRequest);

        // Then
        assertThat(result)
                .isEqualTo("listAllCleaners::page=null,size=null,sortBy=,sortDir=");

        verify(pagingRequest, atLeastOnce()).getPagination();
        verify(pagingRequest, atLeastOnce()).getSorting();
    }

    @Test
    void generate_givenVehicleIdAndPagingRequest_forListCleanersByVehicle_thenBuildsCompositeKey() throws Exception {
        // Given
        Method method = MethodHolder.class.getDeclaredMethod(
                "listCleanersByVehicle",
                String.class,
                CustomPagingRequest.class
        );

        CustomPagingRequest pagingRequest = mock(CustomPagingRequest.class, Answers.RETURNS_DEEP_STUBS);
        when(pagingRequest.getPagination().getPageNumber()).thenReturn(1);
        when(pagingRequest.getPagination().getPageSize()).thenReturn(10);
        when(pagingRequest.getSorting().getSortBy()).thenReturn("id");
        when(pagingRequest.getSorting().getSortDirection()).thenReturn("DESC");

        // When
        Object result = keyGenerator.generate(this, method, "veh-1", pagingRequest);

        // Then
        assertThat(result)
                .isEqualTo("listCleanersByVehicle::veh-1::page=1,size=10,sortBy=id,sortDir=DESC");
    }

    @Test
    void generate_givenFallbackObject_thenUsesStringValueOf() throws Exception {
        // Given
        Method method = MethodHolder.class.getDeclaredMethod("listVehicles", CustomPagingRequest.class);

        // When
        Object result = keyGenerator.generate(this, method, 42);

        // Then
        assertThat(result).isEqualTo("listVehicles::42");
    }

    /**
     * Local method holder used only for reflection.
     * The key generator only depends on method name + params,
     * so these method bodies can stay empty.
     */
    private static class MethodHolder {

        void noArgsMethod() {
        }

        void listVehicles(CustomPagingRequest pagingRequest) {
        }

        void listAllCleaners(CustomPagingRequest pagingRequest) {
        }

        void listCleanersByVehicle(String vehicleId, CustomPagingRequest pagingRequest) {
        }
    }
}