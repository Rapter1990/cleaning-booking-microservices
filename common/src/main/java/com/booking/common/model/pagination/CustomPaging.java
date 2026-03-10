package com.booking.common.model.pagination;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

/**
 * Represents paging parameters for paginated API requests.
 */
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class CustomPaging implements Serializable {

    @Min(value = 1, message = "Page number must be bigger than 0")
    private Integer pageNumber;

    @Min(value = 1, message = "Page size must be bigger than 0")
    private Integer pageSize;

    /**
     * Returns a zero-based page index compatible with Spring Data.
     *
     * @return the page index (zero-based)
     */
    public Integer getPageNumber() {
        return pageNumber - 1;
    }

}
