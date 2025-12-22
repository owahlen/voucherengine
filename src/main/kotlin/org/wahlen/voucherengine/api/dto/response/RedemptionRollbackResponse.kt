package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

data class RedemptionRollbackResponse(
    @field:Schema(description = "Rollback id")
    val id: UUID?,
    @field:Schema(description = "Redemption id")
    val redemption_id: UUID?,
    @field:Schema(description = "Reason for rollback")
    val reason: String?,
    @field:Schema(description = "Amount rolled back")
    val amount: Long?,
    @field:Schema(description = "Created at timestamp")
    val created_at: Instant?
)
