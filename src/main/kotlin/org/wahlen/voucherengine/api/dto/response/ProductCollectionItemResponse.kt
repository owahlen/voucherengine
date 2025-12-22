package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class ProductCollectionItemResponse(
    @field:Schema(description = "Product or SKU identifier", example = "prod_123")
    val id: String? = null,
    @field:Schema(description = "Product ID for SKU entries", example = "prod_123")
    val product_id: String? = null,
    @field:Schema(description = "Object type", example = "product")
    val `object`: String? = null
)
