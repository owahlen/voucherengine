package org.wahlen.voucherengine.persistence.model.redemption

/**
 * High-level outcome of a Voucherengine validation or redemption attempt.
 *
 * Voucherengine API Docs: Redemptions.
 */
enum class RedemptionResult {
    /** The voucher qualified and the redemption was processed. */
    SUCCESS,

    /** The voucher failed qualification or redemption. */
    FAILURE
}
