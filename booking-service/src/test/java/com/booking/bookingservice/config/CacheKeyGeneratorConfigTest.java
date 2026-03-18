package com.booking.bookingservice.config;

import com.booking.common.model.dto.request.CustomPagingRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.cache.interceptor.KeyGenerator;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CacheKeyGeneratorConfigTest {

    private KeyGenerator keyGenerator;

    @BeforeEach
    void setUp() {
        CacheKeyGeneratorConfig config = new CacheKeyGeneratorConfig();
        keyGenerator = config.bookingKeyGenerator();
    }

    @Test
    void bookingKeyGenerator_thenCreatesBean() {
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
    void generate_givenNullAndSimpleTypes_thenBuildsExpectedKey() throws Exception {
        // Given
        Method method = MethodHolder.class.getDeclaredMethod("availabilityByDate", LocalDate.class);

        // When
        Object result = keyGenerator.generate(
                this,
                method,
                null,
                "veh-1",
                2,
                5L
        );

        // Then
        assertThat(result).isEqualTo("availabilityByDate::null::veh-1::2::5");
    }

    @Test
    void generate_givenJavaTimeTypes_thenBuildsExpectedKey() throws Exception {
        // Given
        Method method = MethodHolder.class.getDeclaredMethod(
                "availableCleanersFor",
                String.class,
                LocalDateTime.class,
                int.class
        );

        LocalDate date = LocalDate.of(2026, 3, 16);
        LocalTime time = LocalTime.of(10, 30);
        LocalDateTime dateTime = LocalDateTime.of(2026, 3, 16, 10, 30);

        // When
        Object result = keyGenerator.generate(this, method, date, time, dateTime);

        // Then
        assertThat(result)
                .isEqualTo("availableCleanersFor::2026-03-16::10:30::2026-03-16T10:30");
    }

    @Test
    void generate_givenCustomPagingRequestWithPaginationAndSorting_thenBuildsPagingKey() throws Exception {
        // Given
        Method method = MethodHolder.class.getDeclaredMethod("listVehicles", CustomPagingRequest.class);

        CustomPagingRequest pagingRequest = mock(CustomPagingRequest.class, Answers.RETURNS_DEEP_STUBS);

        when(pagingRequest.getPagination().getPageNumber()).thenReturn(1);
        when(pagingRequest.getPagination().getPageSize()).thenReturn(100);
        when(pagingRequest.getSorting().getSortBy()).thenReturn("id");
        when(pagingRequest.getSorting().getSortDirection()).thenReturn("ASC");

        // When
        Object result = keyGenerator.generate(this, method, pagingRequest);

        // Then
        assertThat(result)
                .isEqualTo("listVehicles::page=1,size=100,sortBy=id,sortDir=ASC");
    }

    @Test
    void generate_givenCustomPagingRequestWithNullPaginationAndSorting_thenBuildsPagingKeyWithDefaults() throws Exception {
        // Given
        Method method = MethodHolder.class.getDeclaredMethod("listVehicles", CustomPagingRequest.class);

        CustomPagingRequest pagingRequest = mock(CustomPagingRequest.class, Answers.RETURNS_DEEP_STUBS);
        when(pagingRequest.getPagination()).thenReturn(null);
        when(pagingRequest.getSorting()).thenReturn(null);

        // When
        Object result = keyGenerator.generate(this, method, pagingRequest);

        // Then
        assertThat(result)
                .isEqualTo("listVehicles::page=null,size=null,sortBy=,sortDir=");
    }

    @Test
    void generate_givenArrayParam_thenBuildsArrayKeyPart() throws Exception {
        // Given
        Method method = MethodHolder.class.getDeclaredMethod("arrayMethod", String[].class);

        // When
        Object result = keyGenerator.generate(this, method, (Object) new String[]{"veh-1", "veh-2"});

        // Then
        assertThat(result).isEqualTo("arrayMethod::[veh-1,veh-2]");
    }

    @Test
    void generate_givenFallbackObject_thenUsesStringValueOf() throws Exception {
        // Given
        Method method = MethodHolder.class.getDeclaredMethod("fallbackMethod", Object.class);

        Object customObject = new Object() {
            @Override
            public String toString() {
                return "custom-object";
            }
        };

        // When
        Object result = keyGenerator.generate(this, method, customObject);

        // Then
        assertThat(result).isEqualTo("fallbackMethod::custom-object");
    }

    private static class MethodHolder {
        void noArgsMethod() {
        }

        void availabilityByDate(LocalDate date) {
        }

        void availableCleanersFor(String vehicleId, LocalDateTime startAt, int durationHours) {
        }

        void listVehicles(CustomPagingRequest pagingRequest) {
        }

        void arrayMethod(String[] values) {
        }

        void fallbackMethod(Object object) {
        }
    }
}