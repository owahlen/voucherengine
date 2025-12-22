package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class ValidationOrderCalculated(
    @field:Schema(description = "Order amount")
    val amount: Long? = null,
    @field:Schema(description = "Initial amount before discounts")
    val initial_amount: Long? = null,
    @field:Schema(description = "Discount amount applied to the order")
    val discount_amount: Long? = null,
    @field:Schema(description = "Item discount amount")
    val items_discount_amount: Long? = null,
    @field:Schema(description = "Total discount amount")
    val total_discount_amount: Long? = null,
    @field:Schema(description = "Order total amount after discounts")
    val total_amount: Long? = null,
    @field:Schema(description = "Applied discount amount")
    val applied_discount_amount: Long? = null,
    @field:Schema(description = "Applied item discount amount")
    val items_applied_discount_amount: Long? = null,
    @field:Schema(description = "Total applied discount amount")
    val total_applied_discount_amount: Long? = null,
    @field:Schema(description = "Order items")
    val items: List<ValidationOrderItemResponse>? = null,
    @field:Schema(description = "Order metadata")
    val metadata: Map<String, Any?>? = null,
    @field:Schema(description = "Object type", example = "order")
    val `object`: String = "order"
)
