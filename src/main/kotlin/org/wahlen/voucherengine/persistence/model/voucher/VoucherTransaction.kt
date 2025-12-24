package org.wahlen.voucherengine.persistence.model.voucher

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.wahlen.voucherengine.persistence.model.common.AuditablePersistable
import org.wahlen.voucherengine.persistence.model.redemption.Redemption
import org.wahlen.voucherengine.persistence.model.redemption.RedemptionRollback
import org.wahlen.voucherengine.persistence.model.tenant.Tenant
import java.util.UUID

/**
 * Represents a transaction on a gift or loyalty voucher.
 *
 * Tracks balance adjustments, redemptions, and rollbacks for audit and reporting.
 * Each transaction records the amount, type, reason, and optional related entities.
 *
 * Voucherengine API Docs: Vouchers → Transactions.
 */
@Entity
class VoucherTransaction(

    /**
     * External source identifier for this transaction.
     */
    @Column
    var sourceId: String? = null,

    /**
     * Voucher to which this transaction belongs.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id", nullable = false)
    var voucher: Voucher? = null,

    /**
     * Campaign ID if this transaction is campaign-related.
     */
    @Column
    var campaignId: UUID? = null,

    /**
     * Type of transaction (CREDITS_ADDITION, CREDITS_REMOVAL, CREDITS_REDEMPTION, POINTS_ADDITION, etc.).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: VoucherTransactionType? = null,

    /**
     * Source of the transaction (e.g., API, ADMIN).
     */
    @Column
    var source: String? = null,

    /**
     * Reason or description for the transaction.
     */
    @Column(columnDefinition = "TEXT")
    var reason: String? = null,

    /**
     * Amount added or subtracted (in smallest currency unit for gift, points for loyalty).
     */
    @Column
    var amount: Long? = null,

    /**
     * Voucher balance after this transaction.
     */
    @Column
    var balanceAfter: Long? = null,

    /**
     * Redemption that caused this transaction (if applicable).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "redemption_id")
    var redemption: Redemption? = null,

    /**
     * Rollback that caused this transaction (if applicable).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rollback_id")
    var rollback: RedemptionRollback? = null,

    /**
     * Related transaction ID (for linking reversals or adjustments).
     */
    @Column
    var relatedTransactionId: UUID? = null,

    /**
     * Additional transaction details as JSON.
     */
    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var details: Map<String, Any?>? = null,

    /**
     * Tenant that owns this transaction.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    var tenant: Tenant? = null

) : AuditablePersistable()
