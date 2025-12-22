package org.wahlen.voucherengine.api.dto.request

import io.swagger.v3.oas.annotations.media.Schema

data class ProductCollectionCreateRequest(
    @field:Schema(description = "Collection name", example = "All Products")
    var name: String? = null,
    @field:Schema(description = "Collection type", example = "STATIC")
    var type: String? = null,
    @field:Schema(description = "Collection filter (for AUTO_UPDATE)")
    var filter: Map<String, Any?>? = null,
    @field:Schema(description = "Collection products (for STATIC)")
    var products: List<ProductCollectionItemRequest>? = null
)
