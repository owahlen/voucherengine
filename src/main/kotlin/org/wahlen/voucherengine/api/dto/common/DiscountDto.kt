package org.wahlen.voucherengine.api.dto.common

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

/**
 * Discount payload as stored in jsonb and exposed over the API.
 */
data class DiscountDto(
    @field:NotNull
    @field:Schema(description = "Discount variant", example = "PERCENT")
    var type: DiscountType,
    @field:Positive
    @field:Schema(description = "Percent off (0-100) for percent discounts", example = "10")
    var percent_off: Int? = null,
    @field:Positive
    @field:Schema(description = "Amount off in minor currency units for amount discounts", example = "500")
    var amount_off: Long? = null,
    @field:Schema(description = "Scope for amount_off (e.g. ORDER)", example = "ORDER")
    var amount_off_type: AmountOffType? = null,
)
