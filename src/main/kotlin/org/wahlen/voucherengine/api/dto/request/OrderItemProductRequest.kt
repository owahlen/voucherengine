package org.wahlen.voucherengine.api.dto.request

import io.swagger.v3.oas.annotations.media.Schema

data class OrderItemProductRequest(
    @field:Schema(description = "Product id")
    var id: String? = null,
    @field:Schema(description = "Product source id")
    var source_id: String? = null,
    @field:Schema(description = "Product name")
    var name: String? = null,
    @field:Schema(description = "Product price")
    var price: Long? = null,
    @field:Schema(description = "Product metadata")
    var metadata: Map<String, Any?>? = null
)
