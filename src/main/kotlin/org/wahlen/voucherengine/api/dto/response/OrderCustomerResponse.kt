package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

data class OrderCustomerResponse(
    @field:Schema(description = "Customer id")
    val id: UUID? = null,
    @field:Schema(description = "Object type", example = "customer")
    val `object`: String = "customer"
)
