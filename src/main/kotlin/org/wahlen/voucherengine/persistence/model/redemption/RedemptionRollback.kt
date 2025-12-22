package org.wahlen.voucherengine.persistence.model.redemption

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.wahlen.voucherengine.persistence.model.common.AbstractPersistable
import org.wahlen.voucherengine.persistence.model.customer.Customer
import org.wahlen.voucherengine.persistence.model.tenant.Tenant
import java.time.Instant
import java.util.UUID

/**
 * Represents a rollback of a Voucherengine redemption, used to reverse a previous redemption.
 *
 * Rollbacks restore voucher balances or usage counters when an order is canceled or a
 * redemption is otherwise invalidated. They are linked to the original redemption event.
 *
 * Voucherengine API Docs: Redemptions.
 */
@Entity
@Table
class RedemptionRollback(
    @Column
    var date: Instant? = null,

    @Column
    var trackingId: String? = null,

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var metadata: Map<String, Any?>? = null,

    @Column
    var amount: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column
    var result: RedemptionResult? = null,

    @Column(columnDefinition = "TEXT")
    var reason: String? = null,

    @Column(name = "redemption_id", insertable = false, updatable = false)
    var redemptionId: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "redemption_id")
    var redemption: Redemption? = null,

    @Column(name = "customer_id", insertable = false, updatable = false)
    var customerId: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    var customer: Customer? = null
) : AbstractPersistable() {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    var tenant: Tenant? = null
}
