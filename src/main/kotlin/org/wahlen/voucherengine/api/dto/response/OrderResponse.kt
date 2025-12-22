package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

data class OrderResponse(
    @field:Schema(description = "Order id", example = "order-123")
    val id: UUID?,
    @field:Schema(description = "Order source id", example = "order-123")
    val source_id: String?,
    @field:Schema(description = "Status", example = "PAID")
    val status: String?,
    @field:Schema(description = "Amount", example = "1500")
    val amount: Long?,
    @field:Schema(description = "Initial amount", example = "2000")
    val initial_amount: Long?,
    @field:Schema(description = "Discount amount", example = "500")
    val discount_amount: Long?,
    @field:Schema(description = "Items discount amount", example = "200")
    val items_discount_amount: Long? = null,
    @field:Schema(description = "Total discount amount", example = "700")
    val total_discount_amount: Long? = null,
    @field:Schema(description = "Total amount after discounts", example = "800")
    val total_amount: Long? = null,
    @field:Schema(description = "Metadata")
    val metadata: Map<String, Any?>? = null,
    @field:Schema(description = "Order items")
    val items: List<OrderItemResponse>? = null,
    @field:Schema(description = "Customer id")
    val customer_id: UUID? = null,
    @field:Schema(description = "Customer object")
    val customer: OrderCustomerResponse? = null,
    @field:Schema(description = "Referrer id")
    val referrer_id: UUID? = null,
    @field:Schema(description = "Created at")
    val created_at: Instant? = null,
    @field:Schema(description = "Updated at")
    val updated_at: Instant? = null,
    @field:Schema(description = "Object type", example = "order")
    val `object`: String = "order"
)
