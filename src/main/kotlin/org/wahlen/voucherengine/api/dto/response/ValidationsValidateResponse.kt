package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.wahlen.voucherengine.api.dto.common.SessionDto

data class ValidationsValidateResponse(
    @field:Schema(description = "Validation id")
    val id: String? = null,
    @field:Schema(description = "Overall validity")
    val valid: Boolean? = null,
    @field:Schema(description = "Redeemables results")
    val redeemables: List<ValidationRedeemableResponse>? = null,
    @field:Schema(description = "Skipped redeemables")
    val skipped_redeemables: List<ValidationRedeemableResponse>? = null,
    @field:Schema(description = "Inapplicable redeemables")
    val inapplicable_redeemables: List<ValidationRedeemableResponse>? = null,
    @field:Schema(description = "Order calculation")
    val order: ValidationOrderCalculated? = null,
    @field:Schema(description = "Tracking id")
    val tracking_id: String? = null,
    @field:Schema(description = "Session")
    val session: SessionDto? = null,
    @field:Schema(description = "Stacking rules")
    val stacking_rules: StackingRulesResponse? = null
)
