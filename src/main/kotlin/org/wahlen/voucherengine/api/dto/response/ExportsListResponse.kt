package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class ExportsListResponse(
    @field:Schema(description = "Object marker", example = "list")
    val `object`: String = "list",
    @field:Schema(description = "Data reference", example = "exports")
    val data_ref: String = "exports",
    @field:Schema(description = "Exports list")
    val exports: List<ExportResponse> = emptyList(),
    @field:Schema(description = "Total number of exports")
    val total: Int = 0
)
