package org.wahlen.voucherengine.api.dto.response

import java.util.UUID

data class RedemptionResponse(
    val result: String,
    val redemptionId: UUID? = null,
    val error: ErrorResponse? = null
)
