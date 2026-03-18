package com.booking.bookingservice.exception;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @Test
    void handleDomain_givenDomainException_thenReturnsConflictProblemDetail() {
        // Given
        DomainException exception = new DomainException("Duration must be 2 or 4 hours.");

        // When
        ProblemDetail result = globalExceptionHandler.handleDomain(exception);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(result.getTitle()).isEqualTo("Booking rule violation");
        assertThat(result.getDetail()).isEqualTo("Duration must be 2 or 4 hours.");
    }

    @Test
    void handleValidation_givenMethodArgumentNotValidException_thenReturnsBadRequestProblemDetail() throws Exception {
        // Given
        Method method = TestController.class.getDeclaredMethod("dummyMethod", String.class);
        MethodParameter methodParameter = new MethodParameter(method, 0);

        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "bookingRequest");
        bindingResult.addError(new FieldError(
                "bookingRequest",
                "startAt",
                "must not be null"
        ));

        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(methodParameter, bindingResult);

        // When
        ProblemDetail result = globalExceptionHandler.handleValidation(exception);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(result.getTitle()).isEqualTo("Validation error");
        assertThat(result.getDetail()).isEqualTo(bindingResult.toString());
    }

    @Test
    void handleConstraint_givenConstraintViolationException_thenReturnsBadRequestProblemDetail() {
        // Given
        ConstraintViolationException exception =
                new ConstraintViolationException("vehicleId must not be blank", Set.of());

        // When
        ProblemDetail result = globalExceptionHandler.handleConstraint(exception);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(result.getTitle()).isEqualTo("Constraint violation");
        assertThat(result.getDetail()).isEqualTo("vehicleId must not be blank");
    }

    private static class TestController {
        @SuppressWarnings("unused")
        void dummyMethod(String request) {
            // only used to create MethodParameter for the test
        }
    }
}