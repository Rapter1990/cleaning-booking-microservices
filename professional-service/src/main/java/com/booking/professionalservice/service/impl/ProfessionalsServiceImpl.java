package com.booking.professionalservice.service.impl;

import com.booking.common.model.dto.request.CustomPagingRequest;
import com.booking.common.model.pagination.CustomPage;
import com.booking.professionalservice.model.dto.request.CleanerDto;
import com.booking.professionalservice.model.dto.request.VehicleDto;
import com.booking.professionalservice.model.entity.CleanerEntity;
import com.booking.professionalservice.model.entity.VehicleEntity;
import com.booking.professionalservice.model.mapper.CleanerEntityToCleanerDtoMapper;
import com.booking.professionalservice.model.mapper.VehicleEntityToVehicleDtoMapper;
import com.booking.professionalservice.repository.CleanerRepository;
import com.booking.professionalservice.repository.VehicleRepository;
import com.booking.professionalservice.service.ProfessionalsService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ProfessionalsServiceImpl implements ProfessionalsService {

  private final VehicleRepository vehicleRepository;
  private final CleanerRepository cleanerRepository;

  private final CleanerEntityToCleanerDtoMapper cleanerMapper =
          CleanerEntityToCleanerDtoMapper.initialize();

  private final VehicleEntityToVehicleDtoMapper vehicleMapper =
          VehicleEntityToVehicleDtoMapper.initialize();

  public ProfessionalsServiceImpl(VehicleRepository vehicleRepository,
                                  CleanerRepository cleanerRepository) {
    this.vehicleRepository = vehicleRepository;
    this.cleanerRepository = cleanerRepository;
  }

  @Override
  @Cacheable(
          cacheNames = "professionals-vehicles",
          keyGenerator = "customPagingKeyGenerator"
  )
  public CustomPage<VehicleDto> listVehicles(final CustomPagingRequest pagingRequest) {

    final Pageable pageable = Optional.ofNullable(pagingRequest)
            .map(CustomPagingRequest::toPageable)
            .orElse(PageRequest.of(0, 20));

    final Page<VehicleEntity> page = vehicleRepository.findAll(pageable);

    final List<String> vehicleIds = page.getContent().stream()
            .map(VehicleEntity::getId)
            .toList();

    final Map<String, List<CleanerDto>> cleanersByVehicleId =
            vehicleIds.isEmpty()
                    ? Map.of()
                    : cleanerRepository.findByVehicle_IdIn(vehicleIds).stream()
                    .map(cleanerMapper::map)
                    .collect(Collectors.groupingBy(CleanerDto::vehicleId));

    final List<VehicleDto> content = page.getContent().stream()
            .map(vehicle -> vehicleMapper.map(
                    vehicle,
                    cleanersByVehicleId.getOrDefault(vehicle.getId(), List.of())
            ))
            .toList();

    return CustomPage.<VehicleDto>builder()
            .content(content)
            .pageNumber(page.getNumber() + 1)
            .pageSize(page.getSize())
            .totalElementCount(page.getTotalElements())
            .totalPageCount(page.getTotalPages())
            .build();
  }

  @Override
  @Cacheable(
          cacheNames = "professionals-cleaners",
          keyGenerator = "customPagingKeyGenerator"
  )
  public CustomPage<CleanerDto> listAllCleaners(final CustomPagingRequest pagingRequest) {

    final Pageable pageable = Optional.ofNullable(pagingRequest)
            .map(CustomPagingRequest::toPageable)
            .orElse(PageRequest.of(0, 20));

    final Page<CleanerEntity> page = cleanerRepository.findAll(pageable);

    final List<CleanerDto> content = page.getContent().stream()
            .map(cleanerMapper::map)
            .toList();

    return CustomPage.<CleanerDto>builder()
            .content(content)
            .pageNumber(page.getNumber() + 1)
            .pageSize(page.getSize())
            .totalElementCount(page.getTotalElements())
            .totalPageCount(page.getTotalPages())
            .build();
  }

  @Override
  @Cacheable(
          cacheNames = "professionals-cleaners-by-vehicle",
          keyGenerator = "customPagingKeyGenerator"
  )
  public CustomPage<CleanerDto> listCleanersByVehicle(final String vehicleId,
                                                      final CustomPagingRequest pagingRequest) {

    final Pageable pageable = Optional.ofNullable(pagingRequest)
            .map(CustomPagingRequest::toPageable)
            .orElse(PageRequest.of(0, 20));

    final Page<CleanerEntity> page = cleanerRepository.findByVehicle_Id(vehicleId, pageable);

    final List<CleanerDto> content = page.getContent().stream()
            .map(cleanerMapper::map)
            .toList();

    return CustomPage.<CleanerDto>builder()
            .content(content)
            .pageNumber(page.getNumber() + 1)
            .pageSize(page.getSize())
            .totalElementCount(page.getTotalElements())
            .totalPageCount(page.getTotalPages())
            .build();
  }
}