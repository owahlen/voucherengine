package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class RedemptionsRedeemResponse(
    @field:Schema(description = "Redemptions")
    val redemptions: List<RedemptionItemResponse>? = null,
    @field:Schema(description = "Order calculation")
    val order: ValidationOrderCalculated? = null,
    @field:Schema(description = "Inapplicable redeemables")
    val inapplicable_redeemables: List<ValidationRedeemableResponse>? = null,
    @field:Schema(description = "Skipped redeemables")
    val skipped_redeemables: List<ValidationRedeemableResponse>? = null
)
