package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class SkusListResponse(
    @field:Schema(description = "Object marker", example = "list")
    val `object`: String = "list",
    @field:Schema(description = "Data reference", example = "data")
    val data_ref: String = "data",
    @field:Schema(description = "SKU list")
    val data: List<SkuResponse> = emptyList(),
    @field:Schema(description = "Total number of SKUs")
    val total: Int = 0
)
