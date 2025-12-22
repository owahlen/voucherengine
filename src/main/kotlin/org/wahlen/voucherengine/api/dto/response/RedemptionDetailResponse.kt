package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.wahlen.voucherengine.service.dto.ErrorResponse
import java.time.Instant
import java.util.UUID

data class RedemptionDetailResponse(
    @field:Schema(description = "Redemption id")
    val id: UUID?,
    @field:Schema(description = "Voucher code")
    val voucher_code: String? = null,
    @field:Schema(description = "Customer id")
    val customer_id: UUID? = null,
    @field:Schema(description = "Amount")
    val amount: Long? = null,
    @field:Schema(description = "Status")
    val status: String? = null,
    @field:Schema(description = "Created at")
    val created_at: Instant? = null,
    @field:Schema(description = "Error payload if any")
    val error: ErrorResponse? = null
)
