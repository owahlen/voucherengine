package org.wahlen.voucherengine.persistence.model.campaign

import jakarta.persistence.*
import org.wahlen.voucherengine.persistence.model.common.AuditablePersistable
import org.wahlen.voucherengine.persistence.model.voucher.Voucher

/**
 * Represents a campaign, which groups vouchers or promotion tiers under a single configuration.
 *
 * Campaigns define the business intent (discounts, gift cards, loyalty, promotions, referrals),
 * how codes are issued (static list, auto-update, or standalone), and whether customers can auto-join.
 * Validation rules and voucher lifecycle constraints typically flow from the campaign to its vouchers.
 */
@Entity
@Table
class Campaign(

    /** Campaign name. */
    @Column(nullable = false)
    var name: String? = null,

    /** An optional field to keep any extra textual information
     *  about the campaign such as a campaign description and details. */
    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    /** Type of campaign. */
    @Enumerated(EnumType.STRING)
    @Column
    var campaignType: CampaignType? = null,

    /** Defines whether the campaign can be updated with new vouchers after campaign creation
     *  or if the campaign consists of generic (standalone) vouchers. */
    @Enumerated(EnumType.STRING)
    @Column
    var mode: CampaignMode? = null,

    /** Indicates whether customers will be able to auto-join a loyalty campaign if any earning rule is fulfilled. */
    @Column
    var autoJoin: Boolean? = null,

    /** If this value is set to true, customers will be able to join the campaign only once.
     *  It is always false for generic (standalone) vouchers campaigns and it cannot be changed in them.
     *  It is always true for loyalty campaigns and it cannot be changed in them. */
    @Column
    var joinOnce: Boolean? = null,

) : AuditablePersistable() {
    @OneToMany(mappedBy = "campaign", cascade = [CascadeType.ALL], orphanRemoval = false, fetch = FetchType.LAZY)
    var vouchers: MutableList<Voucher> = mutableListOf()
}
