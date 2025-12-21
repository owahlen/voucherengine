package org.wahlen.voucherengine.persistence.model.campaign

/**
 * Defines how Voucherengine issues or manages vouchers inside a campaign.
 */
enum class CampaignMode {
    /**
     * Voucherengine auto-generates and updates vouchers in the campaign based on rules and limits.
     */
    AUTO_UPDATE,

    /**
     * A fixed list of vouchers is managed manually or imported, and values do not auto-generate.
     */
    STATIC,

    /**
     * A campaign used for standalone promotions where voucher codes are not required.
     */
    STANDALONE,
}
