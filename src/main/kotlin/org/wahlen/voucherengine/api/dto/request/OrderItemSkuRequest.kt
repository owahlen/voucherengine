package org.wahlen.voucherengine.api.dto.request

import io.swagger.v3.oas.annotations.media.Schema

data class OrderItemSkuRequest(
    @field:Schema(description = "SKU id")
    var id: String? = null,
    @field:Schema(description = "SKU source id")
    var source_id: String? = null,
    @field:Schema(description = "SKU name")
    var sku: String? = null,
    @field:Schema(description = "SKU price")
    var price: Long? = null,
    @field:Schema(description = "SKU metadata")
    var metadata: Map<String, Any?>? = null
)
