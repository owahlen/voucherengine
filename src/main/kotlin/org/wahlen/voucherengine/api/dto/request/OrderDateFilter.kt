package org.wahlen.voucherengine.api.dto.request

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Date filter with conditions.
 */
@Schema(description = "Date filter conditions")
data class OrderDateFilter(
    @Schema(description = "After this date")
    val after: String? = null,

    @Schema(description = "Before this date")
    val before: String? = null
)