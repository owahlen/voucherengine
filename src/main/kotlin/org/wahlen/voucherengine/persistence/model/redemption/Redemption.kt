package org.wahlen.voucherengine.persistence.model.redemption

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.wahlen.voucherengine.persistence.model.common.AuditablePersistable
import org.wahlen.voucherengine.persistence.model.customer.Customer
import org.wahlen.voucherengine.persistence.model.order.Order
import org.wahlen.voucherengine.persistence.model.voucher.Voucher
import org.wahlen.voucherengine.persistence.model.tenant.Tenant
import java.util.UUID

/**
 * Represents a Voucherengine redemption event created after validation or redeeming a voucher.
 *
 * Redemptions record the outcome, amount, customer and order context, and any failure reason.
 * They are used for audit trails, reporting, and enforcing redemption limits.
 *
 * Voucherengine API Docs: Redemptions.
 */
@Entity
@Table
class Redemption(
    /**
     * Hashed customer source ID.
     */
    @Column
    var trackingId: String? = null,

    /**
     * The metadata object stores all custom attributes assigned to the redemption.
     */
    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var metadata: Map<String, Any?>? = null,

    /**
     * For gift cards, this is a positive integer in the smallest currency unit (e.g. 100 cents for $1.00)
     * representing the number of redeemed credits.
     * For loyalty cards, this is the number of loyalty points used in the transaction.
     * Example: 10000
     */
    @Column
    var amount: Long? = null,

    /**
     * Redemption result.
     */
    @Enumerated(EnumType.STRING)
    @Column
    var result: RedemptionResult? = null,

    /**
     * Redemption status.
     */
    @Enumerated(EnumType.STRING)
    @Column
    var status: RedemptionStatus? = null,

    /**
     *
     */
    @Column(columnDefinition = "TEXT")
    var reason: String? = null,

    @Column(name = "voucher_id", insertable = false, updatable = false)
    var voucherId: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id")
    var voucher: Voucher? = null,

    @Column(name = "customer_id", insertable = false, updatable = false)
    var customerId: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    var customer: Customer? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    var order: Order? = null,

    @Column(name = "parent_redemption_id")
    var parentRedemptionId: UUID? = null
) : AuditablePersistable() {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    var tenant: Tenant? = null

    @OneToMany(mappedBy = "redemption", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var rollbacks: MutableList<RedemptionRollback> = mutableListOf()
}
