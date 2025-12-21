package org.wahlen.voucherengine.api.dto.request

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive

data class OrderRequest(
    @field:NotBlank
    var id: String? = null,
    @field:Positive
    var amount: Long? = null,
    var currency: String? = null,
    @field:Valid
    var items: List<OrderItemDto>? = null
)

data class OrderItemDto(
    @field:NotBlank
    var product_id: String? = null,
    @field:Positive
    var quantity: Int? = null,
    @field:Positive
    var price: Long? = null
)
