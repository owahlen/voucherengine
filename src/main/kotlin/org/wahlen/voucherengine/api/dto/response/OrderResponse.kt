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
    @field:Schema(description = "Metadata")
    val metadata: Map<String, Any?>? = null,
    @field:Schema(description = "Customer id")
    val customer_id: UUID? = null,
    @field:Schema(description = "Created at")
    val created_at: Instant? = null,
    @field:Schema(description = "Updated at")
    val updated_at: Instant? = null
)
