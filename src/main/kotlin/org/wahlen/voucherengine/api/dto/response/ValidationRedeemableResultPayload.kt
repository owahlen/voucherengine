package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.wahlen.voucherengine.api.dto.common.DiscountDto

data class ValidationRedeemableResultPayload(
    @field:Schema(description = "Discount payload")
    val discount: DiscountDto? = null,
    @field:Schema(description = "Bundle payload")
    val bundle: QualificationBundle? = null,
    @field:Schema(description = "Gift payload")
    val gift: QualificationGiftResult? = null,
    @field:Schema(description = "Loyalty card payload")
    val loyalty_card: QualificationLoyaltyCardResult? = null,
    @field:Schema(description = "Error payload")
    val error: ValidationErrorDetail? = null,
    @field:Schema(description = "Skip details payload")
    val details: ValidationSkippedDetails? = null
)
