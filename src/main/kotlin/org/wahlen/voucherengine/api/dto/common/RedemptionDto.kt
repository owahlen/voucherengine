package org.wahlen.voucherengine.api.dto.common

import io.swagger.v3.oas.annotations.media.Schema

data class RedemptionDto(
    @field:Schema(description = "Total allowed redemptions for this voucher", example = "1000")
    var quantity: Int? = null,
    @field:Schema(description = "Number of redemptions already used", example = "1")
    var redeemed_quantity: Int? = null,
    @field:Schema(description = "Per-customer redemption limit", example = "1")
    var per_customer: Int? = null
)
