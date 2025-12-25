package org.wahlen.voucherengine.api.dto.request

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Request for exporting orders.
 */
@Schema(description = "Order export request")
data class OrderExportRequest(
    @Schema(
        description = "Export format (CSV or JSON)",
        example = "CSV",
        allowableValues = ["CSV", "JSON"]
    )
    val format: String = "CSV",

    @Schema(
        description = "Filter by order status",
        example = "PAID"
    )
    val status: String? = null,

    @Schema(
        description = "Filter by created after date (ISO 8601)",
        example = "2023-01-01T00:00:00Z"
    )
    val created_after: String? = null,

    @Schema(
        description = "Filter by created before date (ISO 8601)",
        example = "2023-12-31T23:59:59Z"
    )
    val created_before: String? = null
)
