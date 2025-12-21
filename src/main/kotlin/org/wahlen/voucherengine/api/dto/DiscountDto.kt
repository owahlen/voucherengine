package org.wahlen.voucherengine.api.dto

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

/**
 * Discount payload as stored in jsonb and exposed over the API.
 */
data class DiscountDto(
    @field:NotNull
    var type: DiscountType,
    @field:Positive
    var percent_off: Int? = null,
    @field:Positive
    var amount_off: Long? = null,
    var amount_off_type: AmountOffType? = null,
)
