package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class OrdersListResponse(
    @field:Schema(description = "Object marker", example = "list")
    val `object`: String = "list",
    @field:Schema(description = "Data reference", example = "orders")
    val data_ref: String = "orders",
    @field:Schema(description = "Orders list")
    val orders: List<OrderResponse> = emptyList(),
    @field:Schema(description = "Total number of orders")
    val total: Int = 0
)
