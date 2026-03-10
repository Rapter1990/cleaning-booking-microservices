package com.booking.professionalservice.service;

import com.booking.common.model.dto.request.CustomPagingRequest;
import com.booking.common.model.pagination.CustomPage;
import com.booking.professionalservice.model.dto.request.CleanerDto;
import com.booking.professionalservice.model.dto.request.VehicleDto;

public interface ProfessionalsService {

  CustomPage<VehicleDto> listVehicles(CustomPagingRequest pagingRequest);

  CustomPage<CleanerDto> listAllCleaners(CustomPagingRequest pagingRequest);

  CustomPage<CleanerDto> listCleanersByVehicle(String vehicleId, CustomPagingRequest pagingRequest);

}
