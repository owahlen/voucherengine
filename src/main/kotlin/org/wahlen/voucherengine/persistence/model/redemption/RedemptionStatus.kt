package org.wahlen.voucherengine.persistence.model.redemption

/**
 * Detailed status for a Voucherengine redemption record.
 *
 * Voucherengine API Docs: Redemptions.
 */
enum class RedemptionStatus {
    /** Redemption succeeded and consumed voucher value or usage. */
    SUCCEEDED,

    /** Redemption failed and did not consume value. */
    FAILED
}
