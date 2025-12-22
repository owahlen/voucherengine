package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class QualificationLoyaltyCardResult(
    @field:Schema(description = "Loyalty points")
    val points: Long? = null
)
