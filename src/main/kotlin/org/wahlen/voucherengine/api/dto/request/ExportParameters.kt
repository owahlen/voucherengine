package org.wahlen.voucherengine.api.dto.request

import io.swagger.v3.oas.annotations.media.Schema

data class ExportParameters(
    @field:Schema(description = "Sort order, e.g. created_at or -created_at")
    val order: String? = null,
    @field:Schema(description = "List of fields to include in the export")
    val fields: List<String>? = null,
    @field:Schema(description = "Filter conditions")
    val filters: Map<String, Any?>? = null
)
