package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.wahlen.voucherengine.api.dto.common.DiscountDto

data class QualificationRedeemableResult(
    @field:Schema(description = "Discount result for redeemable")
    val discount: DiscountDto? = null
)
