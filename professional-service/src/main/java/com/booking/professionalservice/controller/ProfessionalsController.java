package com.booking.professionalservice.controller;

import com.booking.common.model.dto.request.CustomPagingRequest;
import com.booking.common.model.dto.response.CustomPagingResponse;
import com.booking.professionalservice.model.dto.response.CleanerResponse;
import com.booking.professionalservice.model.dto.response.VehicleResponse;
import com.booking.professionalservice.model.mapper.CustomPageCleanerDtoToCustomPagingCleanerResponseMapper;
import com.booking.professionalservice.model.mapper.CustomPageVehicleDtoToCustomPagingVehicleResponseMapper;
import com.booking.professionalservice.service.ProfessionalsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@Tag(
        name = "Professionals",
        description = "APIs for listing vehicles and cleaners in the professionals service"
)
@RestController
@RequestMapping({"/api", "/api/v1"})
public class ProfessionalsController {

    private final ProfessionalsService professionalsService;

    private final CustomPageCleanerDtoToCustomPagingCleanerResponseMapper cleanerPagingMapper =
            CustomPageCleanerDtoToCustomPagingCleanerResponseMapper.initialize();

    private final CustomPageVehicleDtoToCustomPagingVehicleResponseMapper vehiclePagingMapper =
            CustomPageVehicleDtoToCustomPagingVehicleResponseMapper.initialize();

    public ProfessionalsController(ProfessionalsService professionalsService) {
        this.professionalsService = professionalsService;
    }

    @Operation(
            summary = "List all vehicles",
            description = "Returns all available vehicles with pagination."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Vehicles retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CustomPagingResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content
            )
    })
    @PostMapping("/vehicles")
    public CustomPagingResponse<VehicleResponse> listVehicles(
            @Valid @RequestBody final CustomPagingRequest pagingRequest
    ) {
        return vehiclePagingMapper.toPagingResponse(
                professionalsService.listVehicles(pagingRequest)
        );
    }

    @Operation(
            summary = "List cleaners by vehicle",
            description = "Returns all cleaners assigned to the given vehicle with pagination."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Cleaners retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CustomPagingResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Vehicle not found",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content
            )
    })
    @PostMapping("/vehicles/{vehicleId}/cleaners")
    public CustomPagingResponse<CleanerResponse> listCleanersByVehicle(
            @Parameter(
                    description = "Unique identifier of the vehicle",
                    example = "11111111-1111-1111-1111-111111111111",
                    required = true
            )
            @PathVariable final String vehicleId,
            @Valid @RequestBody final CustomPagingRequest pagingRequest
    ) {
        return cleanerPagingMapper.toPagingResponse(
                professionalsService.listCleanersByVehicle(vehicleId, pagingRequest)
        );
    }

    @Operation(
            summary = "List all cleaners",
            description = "Returns all cleaners across all vehicles with pagination."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Cleaners retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CustomPagingResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content
            )
    })
    @PostMapping("/cleaners")
    public CustomPagingResponse<CleanerResponse> listAllCleaners(
            @Valid @RequestBody final CustomPagingRequest pagingRequest
    ) {
        return cleanerPagingMapper.toPagingResponse(
                professionalsService.listAllCleaners(pagingRequest)
        );
    }

}