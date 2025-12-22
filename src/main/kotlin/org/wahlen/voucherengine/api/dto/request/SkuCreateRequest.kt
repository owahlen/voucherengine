package org.wahlen.voucherengine.api.dto.request

import io.swagger.v3.oas.annotations.media.Schema

data class SkuCreateRequest(
    @field:Schema(description = "SKU name", example = "Premium Monthly")
    var sku: String? = null,
    @field:Schema(description = "SKU source id", example = "sku_premium_monthly")
    var source_id: String? = null,
    @field:Schema(description = "Unit price in minor currency units", example = "10000")
    var price: Long? = null,
    @field:Schema(description = "SKU currency", example = "USD")
    var currency: String? = null,
    @field:Schema(description = "SKU attributes")
    var attributes: Map<String, Any?>? = null,
    @field:Schema(description = "SKU metadata")
    var metadata: Map<String, Any?>? = null,
    @field:Schema(description = "Image URL")
    var image_url: String? = null
)
