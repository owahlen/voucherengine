package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

data class SkuResponse(
    @field:Schema(description = "SKU id")
    val id: UUID?,
    @field:Schema(description = "SKU source id")
    val source_id: String? = null,
    @field:Schema(description = "Product id")
    val product_id: UUID? = null,
    @field:Schema(description = "SKU name")
    val sku: String? = null,
    @field:Schema(description = "Unit price in minor units")
    val price: Long? = null,
    @field:Schema(description = "Currency")
    val currency: String? = null,
    @field:Schema(description = "SKU attributes")
    val attributes: Map<String, Any?>? = null,
    @field:Schema(description = "SKU metadata")
    val metadata: Map<String, Any?>? = null,
    @field:Schema(description = "Image URL")
    val image_url: String? = null,
    @field:Schema(description = "Created at")
    val created_at: Instant? = null,
    @field:Schema(description = "Updated at")
    val updated_at: Instant? = null,
    @field:Schema(description = "Object type", example = "sku")
    val `object`: String = "sku"
)
