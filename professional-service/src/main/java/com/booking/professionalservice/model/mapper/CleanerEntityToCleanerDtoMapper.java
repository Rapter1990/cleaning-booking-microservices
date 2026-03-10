package com.booking.professionalservice.model.mapper;

import com.booking.common.model.mapper.BaseMapper;
import com.booking.professionalservice.model.dto.request.CleanerDto;
import com.booking.professionalservice.model.entity.CleanerEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface CleanerEntityToCleanerDtoMapper extends BaseMapper<CleanerEntity, CleanerDto> {

    @Mapping(source = "vehicle.id", target = "vehicleId")
    CleanerDto map(CleanerEntity source);

    static CleanerEntityToCleanerDtoMapper initialize() {
        return Mappers.getMapper(CleanerEntityToCleanerDtoMapper.class);
    }
}
