package org.wahlen.voucherengine.persistence.model.campaign

/**
 * Represents the business intent of a Voucherengine campaign.
 *
 * Voucherengine API Docs: Campaigns.
 */
enum class CampaignType {
    /** Loyalty program issuing points or balance-based rewards. */
    LOYALTY_PROGRAM,

    /** Gift voucher campaign with stored-value balances. */
    GIFT_VOUCHERS,

    /** Discount coupon campaign with percentage or fixed discounts. */
    DISCOUNT_COUPONS,

    /** Promotion campaign for automatic discounts without unique codes. */
    PROMOTION,

    /** Referral campaign issuing codes and tracking referrers. */
    REFERRAL_PROGRAM,
}
