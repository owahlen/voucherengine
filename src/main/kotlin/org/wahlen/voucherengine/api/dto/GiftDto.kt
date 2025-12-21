package org.wahlen.voucherengine.api.dto

/**
 * Gift voucher payload as stored in jsonb.
 */
data class GiftDto(
    val amount: Long? = null,
    val subtracted_amount: Long? = null,
    val balance: Long? = null,
    val effect: String? = null,
)
