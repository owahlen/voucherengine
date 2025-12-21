package org.wahlen.voucherengine.api.dto

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

/**
 * Discount payload as stored in jsonb and exposed over the API.
 */
data class DiscountDto(
    @field:NotNull
    val type: DiscountType,
    @field:Positive
    val percent_off: Int? = null,
    @field:Positive
    val amount_off: Long? = null,
    val amount_off_type: AmountOffType? = null,
)
