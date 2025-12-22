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
 * Embedded order item snapshot. References product/SKU IDs as soft references.
 */
@Entity
@Table(name = "order_item")
class OrderItem(
    @Column(name = "product_id")
    var productId: String? = null,

    @Column(name = "sku_id")
    var skuId: String? = null,

    @Column(name = "source_id")
    var sourceId: String? = null,

    @Column(name = "related_object")
    var relatedObject: String? = null,

    @Column
    var quantity: Int? = null,

    @Column(name = "amount")
    var amount: Long? = null,

    @Column(name = "discount_amount")
    var discountAmount: Long? = null,

    @Column(name = "subtotal_amount")
    var subtotalAmount: Long? = null,

    @Column
    var price: Long? = null,

    @Column(name = "product_snapshot", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var productSnapshot: Map<String, Any?>? = null,

    @Column(name = "sku_snapshot", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var skuSnapshot: Map<String, Any?>? = null,

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var metadata: Map<String, Any?>? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    var order: Order? = null
) : AuditablePersistable() {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    var tenant: Tenant? = null
}
