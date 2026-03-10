package com.booking.professionalservice.model.dto.response;

import java.io.Serializable;

public record CleanerResponse(
        String id,
        String fullName,
        String vehicleId
) implements Serializable {}
