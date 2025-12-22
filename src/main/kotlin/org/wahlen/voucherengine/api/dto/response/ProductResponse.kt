package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

data class ProductResponse(
    @field:Schema(description = "Product id")
    val id: UUID?,
    @field:Schema(description = "Product source id")
    val source_id: String? = null,
    @field:Schema(description = "Product name")
    val name: String? = null,
    @field:Schema(description = "Unit price in minor units")
    val price: Long? = null,
    @field:Schema(description = "Product attributes")
    val attributes: List<String>? = null,
    @field:Schema(description = "Product metadata")
    val metadata: Map<String, Any?>? = null,
    @field:Schema(description = "Image URL")
    val image_url: String? = null,
    @field:Schema(description = "Created at")
    val created_at: Instant? = null,
    @field:Schema(description = "Updated at")
    val updated_at: Instant? = null,
    @field:Schema(description = "Object type", example = "product")
    val `object`: String = "product",
    @field:Schema(description = "Product SKUs")
    val skus: SkusListResponse? = null
)
