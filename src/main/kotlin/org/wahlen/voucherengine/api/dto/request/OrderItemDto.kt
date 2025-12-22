package org.wahlen.voucherengine.api.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive

data class OrderItemDto(
    @field:NotBlank
    @field:Schema(description = "Product identifier", example = "sku-1")
    var product_id: String? = null,
    @field:Positive
    @field:Schema(description = "Quantity of the product", example = "2")
    var quantity: Int? = null,
    @field:Positive
    @field:Schema(description = "Price per item in minor currency units", example = "500")
    var price: Long? = null
)
