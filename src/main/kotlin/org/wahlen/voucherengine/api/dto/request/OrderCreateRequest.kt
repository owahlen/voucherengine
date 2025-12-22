package org.wahlen.voucherengine.api.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive

data class OrderCreateRequest(
    @field:NotBlank
    @field:Schema(description = "Order source identifier", example = "order-123")
    var source_id: String? = null,
    @field:Schema(description = "Order status", example = "PAID")
    var status: String? = null,
    @field:Positive
    @field:Schema(description = "Order amount in minor units", example = "1500")
    var amount: Long? = null,
    @field:Schema(description = "Initial amount before discounts", example = "2000")
    var initial_amount: Long? = null,
    @field:Schema(description = "Discount amount applied", example = "500")
    var discount_amount: Long? = null,
    @field:Schema(description = "Order metadata")
    var metadata: Map<String, Any?>? = null,
    @field:Valid
    @field:Schema(description = "Order items")
    var items: List<OrderItemDto>? = null,
    @field:Valid
    @field:Schema(description = "Customer reference for the order")
    var customer: CustomerReferenceDto? = null
)
