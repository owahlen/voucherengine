package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class OrderItemSkuResponse(
    @field:Schema(description = "SKU id")
    val id: String? = null,
    @field:Schema(description = "SKU source id")
    val source_id: String? = null,
    @field:Schema(description = "SKU name")
    val sku: String? = null,
    @field:Schema(description = "SKU price")
    val price: Long? = null,
    @field:Schema(description = "SKU metadata")
    val metadata: Map<String, Any?>? = null
)
