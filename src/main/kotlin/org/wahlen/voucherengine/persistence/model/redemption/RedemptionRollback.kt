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
class RedemptionRollback(

    /**
     * Timestamp when the rollback was performed.
     */
    @Column
    var date: Instant? = null,

    /**
     * Hashed tracking identifier for privacy.
     */
    @Column
    var trackingId: String? = null,

    /**
     * The metadata object stores all custom attributes assigned to the rollback.
     */
    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var metadata: Map<String, Any?>? = null,

    /**
     * Amount restored (for gift/loyalty vouchers).
     */
    @Column
    var amount: Long? = null,

    /**
     * Rollback result (SUCCESS or FAILURE).
     */
    @Enumerated(EnumType.STRING)
    @Column
    var result: RedemptionResult? = null,

    /**
     * Reason for the rollback.
     */
    @Column(columnDefinition = "TEXT")
    var reason: String? = null,

    /**
     * Original redemption being rolled back.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "redemption_id")
    var redemption: Redemption? = null,

    /**
     * Customer who triggered the rollback (may differ from original redeemer).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    var customer: Customer? = null,

    /**
     * Tenant that owns this rollback.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    var tenant: Tenant? = null

) : AbstractPersistable()
