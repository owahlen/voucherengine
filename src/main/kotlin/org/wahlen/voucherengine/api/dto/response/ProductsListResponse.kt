package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class ProductsListResponse(
    @field:Schema(description = "Object marker", example = "list")
    val `object`: String = "list",
    @field:Schema(description = "Data reference", example = "products")
    val data_ref: String = "products",
    @field:Schema(description = "Products list")
    val products: List<ProductResponse> = emptyList(),
    @field:Schema(description = "Total number of products")
    val total: Int = 0
)
