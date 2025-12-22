package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class ProductCollectionsProductsListResponse(
    @field:Schema(description = "Object marker", example = "list")
    val `object`: String = "list",
    @field:Schema(description = "Data reference", example = "data")
    val data_ref: String = "data",
    @field:Schema(description = "Products and SKUs in collection")
    val data: List<Any> = emptyList(),
    @field:Schema(description = "Total number of entries")
    val total: Int = 0
)
