package org.wahlen.voucherengine.persistence.model.voucher

enum class VoucherTransactionType {
    CREDITS_ADDITION,
    CREDITS_REMOVAL,
    CREDITS_REFUND,
    CREDITS_REDEMPTION,
    POINTS_ADDITION,
    POINTS_REMOVAL,
    POINTS_REFUND,
    POINTS_REDEMPTION,
    POINTS_TRANSFER_IN,
    POINTS_TRANSFER_OUT,
    POINTS_EXPIRATION
}
