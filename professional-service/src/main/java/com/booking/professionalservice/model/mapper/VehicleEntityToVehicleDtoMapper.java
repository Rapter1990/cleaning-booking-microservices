package com.booking.professionalservice.model.mapper;

import com.booking.common.model.mapper.BaseMapper;
import com.booking.professionalservice.model.dto.request.CleanerDto;
import com.booking.professionalservice.model.dto.request.VehicleDto;
import com.booking.professionalservice.model.entity.VehicleEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface VehicleEntityToVehicleDtoMapper extends BaseMapper<VehicleEntity, VehicleDto> {

    @Mapping(source = "source.id", target = "id")
    @Mapping(source = "source.code", target = "code")
    @Mapping(source = "source.licensePlate", target = "licensePlate")
    @Mapping(source = "cleaners", target = "cleaners")
    VehicleDto map(VehicleEntity source, List<CleanerDto> cleaners);

    static VehicleEntityToVehicleDtoMapper initialize() {
        return Mappers.getMapper(VehicleEntityToVehicleDtoMapper.class);
    }
}
