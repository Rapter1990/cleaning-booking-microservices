package com.booking.professionalservice.model.dto.response;

import java.io.Serializable;
import java.util.List;

public record VehicleResponse(
        String id,
        String code,
        String licensePlate,
        List<CleanerResponse> cleaners
) implements Serializable {}
