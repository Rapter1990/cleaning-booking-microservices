package com.booking.professionalservice.model.mapper;

import com.booking.common.model.mapper.BaseMapper;
import com.booking.professionalservice.model.dto.request.VehicleDto;
import com.booking.professionalservice.model.dto.response.VehicleResponse;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(uses = CleanerDtoToCleanerResponseMapper.class)
public interface VehicleDtoToVehicleResponseMapper extends BaseMapper<VehicleDto, VehicleResponse> {

    VehicleResponse map(VehicleDto source);

    static VehicleDtoToVehicleResponseMapper initialize() {
        return Mappers.getMapper(VehicleDtoToVehicleResponseMapper.class);
    }

}
