package org.wahlen.voucherengine.api.dto.request

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Export filter conditions.
 */
@Schema(description = "Order export filters")
data class OrderExportFilters(
    @Schema(
        description = "Filter by order status",
        example = "PAID",
        allowableValues = ["CREATED", "PAID", "FULFILLED", "CANCELED"]
    )
    val status: String? = null,

    @Schema(
        description = "Filter by created after date (ISO 8601)",
        example = "2023-01-01T00:00:00Z"
    )
    val created_at: OrderDateFilter? = null,

    @Schema(
        description = "Filter by updated after date (ISO 8601)"
    )
    val updated_at: OrderDateFilter? = null
)