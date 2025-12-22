package org.wahlen.voucherengine.api.dto.common

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Gift voucher payload as stored in jsonb.
 */
data class GiftDto(
    @field:Schema(description = "Initial gift amount in minor currency units", example = "5000")
    var amount: Long? = null,
    @field:Schema(description = "Amount already used", example = "1500")
    var subtracted_amount: Long? = null,
    @field:Schema(description = "Current balance after redemptions", example = "3500")
    var balance: Long? = null,
    @field:Schema(description = "Effect applied to balance (e.g. ADD_BALANCE)", example = "ADD_BALANCE")
    var effect: String? = null,
)
