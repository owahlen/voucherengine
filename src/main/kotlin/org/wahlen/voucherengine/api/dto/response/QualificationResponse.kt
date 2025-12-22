package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class QualificationResponse(
    @field:Schema(description = "Redeemables list")
    val redeemables: QualificationRedeemables,
    @field:Schema(description = "Tracking id")
    val tracking_id: String? = null,
    @field:Schema(description = "Order summary")
    val order: QualificationOrderSummary? = null,
    @field:Schema(description = "Stacking rules")
    val stacking_rules: StackingRulesResponse? = null
)
