package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class OrderItemResponse(
    @field:Schema(description = "Product identifier", example = "prod_123")
    val product_id: String? = null,
    @field:Schema(description = "SKU identifier", example = "sku_456")
    val sku_id: String? = null,
    @field:Schema(description = "Quantity", example = "2")
    val quantity: Int? = null,
    @field:Schema(description = "Unit price in minor units", example = "10000")
    val price: Long? = null
)
