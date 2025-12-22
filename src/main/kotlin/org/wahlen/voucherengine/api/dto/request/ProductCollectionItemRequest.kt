package org.wahlen.voucherengine.api.dto.request

import io.swagger.v3.oas.annotations.media.Schema

data class ProductCollectionItemRequest(
    @field:Schema(description = "Product or SKU identifier", example = "prod_123")
    var id: String? = null,
    @field:Schema(description = "Product ID for SKU entries", example = "prod_123")
    var product_id: String? = null,
    @field:Schema(description = "Object type", example = "product")
    var `object`: String? = null
)
