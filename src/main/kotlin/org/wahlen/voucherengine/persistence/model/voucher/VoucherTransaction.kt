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
    @Column
    var sourceId: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id", nullable = false)
    var voucher: Voucher? = null,

    @Column
    var campaignId: UUID? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: VoucherTransactionType? = null,

    @Column
    var source: String? = null,

    @Column(columnDefinition = "TEXT")
    var reason: String? = null,

    @Column
    var amount: Long? = null,

    @Column
    var balanceAfter: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "redemption_id")
    var redemption: Redemption? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rollback_id")
    var rollback: RedemptionRollback? = null,

    @Column
    var relatedTransactionId: UUID? = null,

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var details: Map<String, Any?>? = null

) : AuditablePersistable() {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    var tenant: Tenant? = null
}
