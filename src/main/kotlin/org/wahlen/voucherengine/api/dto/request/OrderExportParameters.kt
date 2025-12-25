package org.wahlen.voucherengine.api.dto.request

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Export parameters wrapper.
 */
@Schema(description = "Export parameters")
data class OrderExportParameters(
    @Schema(
        description = "Array of field names to include in export",
        example = "[\"id\", \"source_id\", \"status\", \"created_at\", \"amount\", \"customer_id\"]"
    )
    val fields: List<String>? = null,

    @Schema(
        description = "Filter conditions for orders to export"
    )
    val filters: OrderExportFilters? = null,

    @Schema(
        description = "Sort order (e.g., '-created_at' for descending, 'created_at' for ascending)",
        example = "-created_at"
    )
    val order: String? = null,

    @Schema(
        description = "Export format (CSV or JSON)",
        example = "CSV",
        allowableValues = ["CSV", "JSON"]
    )
    val format: String? = null
)