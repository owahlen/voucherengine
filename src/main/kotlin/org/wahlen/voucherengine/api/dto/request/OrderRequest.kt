package org.wahlen.voucherengine.api.dto.request

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive

data class OrderRequest(
    @field:NotBlank
    val id: String? = null,
    @field:Positive
    val amount: Long? = null,
    val currency: String? = null,
    @field:Valid
    val items: List<OrderItemDto>? = null
)

data class OrderItemDto(
    @field:NotBlank
    val product_id: String? = null,
    @field:Positive
    val quantity: Int? = null,
    @field:Positive
    val price: Long? = null
)
