package org.wahlen.voucherengine.persistence.model.publication

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.wahlen.voucherengine.persistence.model.campaign.Campaign
import org.wahlen.voucherengine.persistence.model.common.AuditablePersistable
import org.wahlen.voucherengine.persistence.model.customer.Customer
import org.wahlen.voucherengine.persistence.model.tenant.Tenant
import org.wahlen.voucherengine.persistence.model.voucher.Voucher

/**
 * Represents a voucher publication event to a customer or channel.
 *
 * Publications track voucher distribution results, whether via API, SMS, email, or other channels.
 * They capture success/failure status and link the published voucher(s) to a customer or campaign.
 *
 * Voucherengine API Docs: Publications.
 */
@Entity
class Publication(

    /**
     * Single voucher published (for simple publication).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id")
    var voucher: Voucher? = null,

    /**
     * Multiple vouchers published (for batch publication).
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "publication_vouchers",
        joinColumns = [JoinColumn(name = "publication_id")],
        inverseJoinColumns = [JoinColumn(name = "voucher_id")]
    )
    var vouchers: MutableSet<Voucher> = mutableSetOf(),

    /**
     * Customer receiving the voucher(s).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    var customer: Customer? = null,

    /**
     * Campaign from which voucher(s) were published.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    var campaign: Campaign? = null,

    /**
     * Publication result (success or failure).
     */
    @Enumerated(EnumType.STRING)
    @Column
    var result: PublicationResult? = null,

    /**
     * Error code if publication failed.
     */
    @Column(name = "failure_code")
    var failureCode: String? = null,

    /**
     * Detailed failure message.
     */
    @Column(name = "failure_message", columnDefinition = "TEXT")
    var failureMessage: String? = null,

    /**
     * External source identifier for tracking.
     */
    @Column(name = "source_id")
    var sourceId: String? = null,

    /**
     * Distribution channel (e.g., API, SMS, Email).
     */
    @Column
    var channel: String? = null,

    /**
     * The metadata object stores all custom attributes assigned to the publication.
     */
    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var metadata: Map<String, Any?>? = null,

    /**
     * Tenant that owns this publication.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    var tenant: Tenant? = null

) : AuditablePersistable()
