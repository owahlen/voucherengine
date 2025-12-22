package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class OrderItemProductResponse(
    @field:Schema(description = "Product id")
    val id: String? = null,
    @field:Schema(description = "Product source id")
    val source_id: String? = null,
    @field:Schema(description = "Product name")
    val name: String? = null,
    @field:Schema(description = "Product price")
    val price: Long? = null,
    @field:Schema(description = "Product metadata")
    val metadata: Map<String, Any?>? = null
)
