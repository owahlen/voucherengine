package org.wahlen.voucherengine.api.dto

data class RedemptionDto(
    val quantity: Int? = null,
    val redeemed_quantity: Int? = null,
    val per_customer: Int? = null
)
