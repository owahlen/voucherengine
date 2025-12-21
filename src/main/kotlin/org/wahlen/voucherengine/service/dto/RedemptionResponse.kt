package org.wahlen.voucherengine.service.dto

import java.util.UUID

data class RedemptionResponse(
    val result: String,
    val redemptionId: UUID? = null,
    val error: ErrorResponse? = null
)
