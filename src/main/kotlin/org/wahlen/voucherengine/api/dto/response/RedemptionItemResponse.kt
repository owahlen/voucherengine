package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

data class RedemptionItemResponse(
    @field:Schema(description = "Redemption id")
    val id: UUID? = null,
    @field:Schema(description = "Object type", example = "redemption")
    val `object`: String = "redemption",
    @field:Schema(description = "Redemption date")
    val date: Instant? = null,
    @field:Schema(description = "Customer id")
    val customer_id: UUID? = null,
    @field:Schema(description = "Tracking id")
    val tracking_id: String? = null,
    @field:Schema(description = "Redemption metadata")
    val metadata: Map<String, Any?>? = null,
    @field:Schema(description = "Redeemed amount")
    val amount: Long? = null,
    @field:Schema(description = "Redemption result")
    val result: String? = null,
    @field:Schema(description = "Redemption status")
    val status: String? = null,
    @field:Schema(description = "Failure code")
    val failure_code: String? = null,
    @field:Schema(description = "Failure message")
    val failure_message: String? = null,
    @field:Schema(description = "Session lock")
    val session: org.wahlen.voucherengine.api.dto.common.SessionDto? = null
)
