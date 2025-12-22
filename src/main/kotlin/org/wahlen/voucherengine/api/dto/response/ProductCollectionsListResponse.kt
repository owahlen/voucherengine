package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class ProductCollectionsListResponse(
    @field:Schema(description = "Object marker", example = "list")
    val `object`: String = "list",
    @field:Schema(description = "Data reference", example = "data")
    val data_ref: String = "data",
    @field:Schema(description = "Product collections list")
    val data: List<ProductCollectionResponse> = emptyList(),
    @field:Schema(description = "Total number of collections")
    val total: Int = 0
)
