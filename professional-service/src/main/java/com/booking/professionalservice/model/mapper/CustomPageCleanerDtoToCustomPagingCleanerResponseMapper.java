package com.booking.professionalservice.model.mapper;

import com.booking.common.model.dto.response.CustomPagingResponse;
import com.booking.common.model.pagination.CustomPage;
import com.booking.professionalservice.model.dto.request.CleanerDto;
import com.booking.professionalservice.model.dto.response.CleanerResponse;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface CustomPageCleanerDtoToCustomPagingCleanerResponseMapper {

    CleanerDtoToCleanerResponseMapper ITEM_MAPPER =
            Mappers.getMapper(CleanerDtoToCleanerResponseMapper.class);

    default CustomPagingResponse<CleanerResponse> toPagingResponse(CustomPage<CleanerDto> page) {
        if (page == null) return null;

        return CustomPagingResponse.<CleanerResponse>builder()
                .content(toResponseList(page.getContent()))
                .totalElementCount(page.getTotalElementCount())
                .totalPageCount(page.getTotalPageCount())
                .pageNumber(page.getPageNumber())
                .pageSize(page.getPageSize())
                .build();
    }

    default List<CleanerResponse> toResponseList(List<CleanerDto> list) {
        if (list == null) return List.of();
        return list.stream().map(ITEM_MAPPER::map).toList();
    }

    static CustomPageCleanerDtoToCustomPagingCleanerResponseMapper initialize() {
        return Mappers.getMapper(CustomPageCleanerDtoToCustomPagingCleanerResponseMapper.class);
    }
}
