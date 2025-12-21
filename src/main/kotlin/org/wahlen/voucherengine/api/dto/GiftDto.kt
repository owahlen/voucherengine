package org.wahlen.voucherengine.api.dto

/**
 * Gift voucher payload as stored in jsonb.
 */
data class GiftDto(
    var amount: Long? = null,
    var subtracted_amount: Long? = null,
    var balance: Long? = null,
    var effect: String? = null,
)
