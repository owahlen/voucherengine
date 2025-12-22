package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

data class ProductCollectionResponse(
    @field:Schema(description = "Collection id")
    val id: UUID? = null,
    @field:Schema(description = "Collection name")
    val name: String? = null,
    @field:Schema(description = "Collection type")
    val type: String? = null,
    @field:Schema(description = "Collection filter")
    val filter: Map<String, Any?>? = null,
    @field:Schema(description = "Collection products")
    val products: List<ProductCollectionItemResponse>? = null,
    @field:Schema(description = "Created at")
    val created_at: Instant? = null,
    @field:Schema(description = "Object type", example = "products_collection")
    val `object`: String = "products_collection"
)
