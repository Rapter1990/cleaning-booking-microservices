package com.booking.professionalservice.model.mapper;

import com.booking.common.model.mapper.BaseMapper;
import com.booking.professionalservice.model.dto.request.CleanerDto;
import com.booking.professionalservice.model.dto.response.CleanerResponse;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface CleanerDtoToCleanerResponseMapper extends BaseMapper<CleanerDto, CleanerResponse> {

    CleanerResponse map(CleanerDto source);

    static CleanerDtoToCleanerResponseMapper initialize() {
        return Mappers.getMapper(CleanerDtoToCleanerResponseMapper.class);
    }

}
