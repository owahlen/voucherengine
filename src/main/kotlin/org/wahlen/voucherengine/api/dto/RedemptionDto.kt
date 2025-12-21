package org.wahlen.voucherengine.api.dto

data class RedemptionDto(
    var quantity: Int? = null,
    var redeemed_quantity: Int? = null,
    var per_customer: Int? = null
)
