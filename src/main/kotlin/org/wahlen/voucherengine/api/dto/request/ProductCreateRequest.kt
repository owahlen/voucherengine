package org.wahlen.voucherengine.api.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class ProductCreateRequest(
    @field:NotBlank
    @field:Schema(description = "Product name", example = "Premium Plan")
    var name: String? = null,
    @field:Schema(description = "Product source id", example = "prod_premium")
    var source_id: String? = null,
    @field:Schema(description = "Unit price in minor currency units", example = "10000")
    var price: Long? = null,
    @field:Schema(description = "Product attributes", example = """["color","size"]""")
    var attributes: List<String>? = null,
    @field:Schema(description = "Product metadata")
    var metadata: Map<String, Any?>? = null,
    @field:Schema(description = "Image URL")
    var image_url: String? = null
)
