package com.booking.professionalservice.model.mapper;

import com.booking.common.model.dto.response.CustomPagingResponse;
import com.booking.common.model.pagination.CustomPage;
import com.booking.professionalservice.model.dto.request.VehicleDto;
import com.booking.professionalservice.model.dto.response.VehicleResponse;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface CustomPageVehicleDtoToCustomPagingVehicleResponseMapper {

    VehicleDtoToVehicleResponseMapper ITEM_MAPPER =
            Mappers.getMapper(VehicleDtoToVehicleResponseMapper.class);

    default CustomPagingResponse<VehicleResponse> toPagingResponse(CustomPage<VehicleDto> page) {
        if (page == null) return null;

        return CustomPagingResponse.<VehicleResponse>builder()
                .content(toResponseList(page.getContent()))
                .totalElementCount(page.getTotalElementCount())
                .totalPageCount(page.getTotalPageCount())
                .pageNumber(page.getPageNumber())
                .pageSize(page.getPageSize())
                .build();
    }

    default List<VehicleResponse> toResponseList(List<VehicleDto> list) {
        if (list == null) return List.of();
        return list.stream().map(ITEM_MAPPER::map).toList();
    }

    static CustomPageVehicleDtoToCustomPagingVehicleResponseMapper initialize() {
        return Mappers.getMapper(CustomPageVehicleDtoToCustomPagingVehicleResponseMapper.class);
    }
}
