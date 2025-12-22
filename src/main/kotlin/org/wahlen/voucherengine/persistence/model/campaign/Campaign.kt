package org.wahlen.voucherengine.persistence.model.campaign

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.wahlen.voucherengine.persistence.model.common.AuditablePersistable
import org.wahlen.voucherengine.persistence.model.tenant.Tenant
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
    @Column(name = "type")
    var type: CampaignType? = null,

    /** Defines whether the campaign can be updated with new vouchers after campaign creation
     *  or if the campaign consists of generic (standalone) vouchers. */
    @Enumerated(EnumType.STRING)
    @Column
    var mode: CampaignMode? = null,

    /** Code pattern to be used when issuing vouchers under this campaign. */
    @Column(name = "code_pattern")
    var codePattern: String? = null,

    @Column(name = "start_date")
    var startDate: java.time.Instant? = null,

    @Column(name = "expiration_date")
    var expirationDate: java.time.Instant? = null,

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var metadata: Map<String, Any?>? = null,

    @Column
    var active: Boolean? = true,

) : AuditablePersistable() {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    var tenant: Tenant? = null

    @OneToMany(mappedBy = "campaign", cascade = [CascadeType.ALL], orphanRemoval = false, fetch = FetchType.LAZY)
    var vouchers: MutableList<Voucher> = mutableListOf()
}
