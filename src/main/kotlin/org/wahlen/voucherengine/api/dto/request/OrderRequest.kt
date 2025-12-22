package org.wahlen.voucherengine.api.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive

data class OrderRequest(
    @field:NotBlank
    @field:Schema(description = "Order identifier", example = "order-1")
    var id: String? = null,
    @field:Positive
    @field:Schema(description = "Order amount in minor currency units", example = "1500")
    var amount: Long? = null,
    @field:Schema(description = "ISO currency code", example = "USD")
    var currency: String? = null,
    @field:Valid
    @field:Schema(description = "Line items for the order")
    var items: List<OrderItemDto>? = null,
    @field:Schema(description = "Arbitrary order metadata for validation rules", example = """{"channel":"mobile","is_test":false}""")
    var metadata: Map<String, Any?>? = null
)

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
