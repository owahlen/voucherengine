package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.wahlen.voucherengine.api.dto.common.DiscountDto

data class QualificationRedeemableResult(
    @field:Schema(description = "Discount result for redeemable")
    val discount: DiscountDto? = null,
    @field:Schema(description = "Bundle result")
    val bundle: QualificationBundle? = null,
    @field:Schema(description = "Gift result")
    val gift: QualificationGiftResult? = null,
    @field:Schema(description = "Loyalty card result")
    val loyalty_card: QualificationLoyaltyCardResult? = null,
    @field:Schema(description = "Error result")
    val error: QualificationErrorResult? = null
)
