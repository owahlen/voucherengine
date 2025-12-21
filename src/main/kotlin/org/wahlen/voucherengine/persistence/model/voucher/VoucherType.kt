package org.wahlen.voucherengine.persistence.model.voucher

/**
 * Identifies the Voucherengine voucher subtype and its value model.
 *
 * Voucherengine API Docs: Vouchers.
 */
enum class VoucherType {
    /** A voucher carrying percentage or fixed-amount discounts. */
    DISCOUNT_VOUCHER,

    /** A stored-value voucher that can be partially redeemed. */
    GIFT_VOUCHER,

    /** A loyalty card linked to an accumulating balance or points. */
    LOYALTY_CARD
}
