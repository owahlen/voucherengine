package org.wahlen.voucherengine.persistence.model.order

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.wahlen.voucherengine.persistence.model.common.AuditablePersistable
import org.wahlen.voucherengine.persistence.model.customer.Customer
import org.wahlen.voucherengine.persistence.model.redemption.Redemption
import org.wahlen.voucherengine.persistence.model.tenant.Tenant

/**
 * Represents an order for validation and redemption context.
 *
 * Orders contain line items, amounts, and customer references to enable
 * voucher validation rules based on cart contents, totals, or product eligibility.
 *
 * Voucherengine API Docs: Orders.
 */
@Entity
@Table(name = "orders")
class Order(

    /**
     * External source identifier for this order.
     */
    @Column
    var sourceId: String? = null,

    /**
     * Order status (e.g., CREATED, PAID, CANCELED).
     */
    @Column
    var status: String? = null,

    /**
     * Final order amount in smallest currency unit (e.g., cents).
     */
    @Column
    var amount: Long? = null,

    /**
     * Initial order amount before discounts.
     */
    @Column
    var initialAmount: Long? = null,

    /**
     * Total discount applied to this order.
     */
    @Column
    var discountAmount: Long? = null,

    /**
     * The metadata object stores all custom attributes assigned to the order.
     */
    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var metadata: Map<String, Any?>? = null,

    /**
     * Customer who placed this order.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    var customer: Customer? = null,

    /**
     * Tenant that owns this order.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    var tenant: Tenant? = null,

    /**
     * Redemptions associated with this order.
     */
    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = false, fetch = FetchType.LAZY)
    var redemptions: MutableList<Redemption> = mutableListOf(),

    /**
     * Line items in this order.
     */
    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var items: MutableList<OrderItem> = mutableListOf()

) : AuditablePersistable()
