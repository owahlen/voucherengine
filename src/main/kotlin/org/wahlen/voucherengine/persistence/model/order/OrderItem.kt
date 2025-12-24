package org.wahlen.voucherengine.persistence.model.order

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.wahlen.voucherengine.persistence.model.common.AuditablePersistable
import org.wahlen.voucherengine.persistence.model.tenant.Tenant

/**
 * Represents a line item in an order for validation context.
 *
 * Order items capture product/SKU snapshots at order time and track quantities,
 * amounts, and discounts applied. Used by validation rules to check cart composition.
 *
 * Voucherengine API Docs: Orders.
 */
@Entity
@Table(name = "order_item")
class OrderItem(

    /**
     * Product ID reference (soft reference to Product entity or external ID).
     */
    @Column(name = "product_id")
    var productId: String? = null,

    /**
     * SKU ID reference (soft reference to SKU entity or external ID).
     */
    @Column(name = "sku_id")
    var skuId: String? = null,

    /**
     * External source identifier for this item.
     */
    @Column(name = "source_id")
    var sourceId: String? = null,

    /**
     * Related object (e.g., subscription, bundle ID).
     */
    @Column(name = "related_object")
    var relatedObject: String? = null,

    /**
     * Quantity of this item in the order.
     */
    @Column
    var quantity: Int? = null,

    /**
     * Total amount for this line item (quantity × price).
     */
    @Column(name = "amount")
    var amount: Long? = null,

    /**
     * Discount applied to this line item.
     */
    @Column(name = "discount_amount")
    var discountAmount: Long? = null,

    /**
     * Subtotal after discount.
     */
    @Column(name = "subtotal_amount")
    var subtotalAmount: Long? = null,

    /**
     * Unit price for this item.
     */
    @Column
    var price: Long? = null,

    /**
     * Snapshot of product data at order time.
     */
    @Column(name = "product_snapshot", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var productSnapshot: Map<String, Any?>? = null,

    /**
     * Snapshot of SKU data at order time.
     */
    @Column(name = "sku_snapshot", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var skuSnapshot: Map<String, Any?>? = null,

    /**
     * The metadata object stores all custom attributes assigned to the order item.
     */
    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var metadata: Map<String, Any?>? = null,

    /**
     * Order this item belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    var order: Order? = null,

    /**
     * Tenant that owns this order item.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    var tenant: Tenant? = null

) : AuditablePersistable()
