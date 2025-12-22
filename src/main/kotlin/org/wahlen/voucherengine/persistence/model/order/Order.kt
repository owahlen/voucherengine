package org.wahlen.voucherengine.persistence.model.order

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.wahlen.voucherengine.persistence.model.common.AuditablePersistable
import org.wahlen.voucherengine.persistence.model.customer.Customer
import org.wahlen.voucherengine.persistence.model.redemption.Redemption
import org.wahlen.voucherengine.persistence.model.tenant.Tenant

@Entity
@Table(name = "orders")
class Order(
    @Column
    var sourceId: String? = null,

    @Column
    var status: String? = null,

    @Column
    var amount: Long? = null,

    @Column
    var initialAmount: Long? = null,

    @Column
    var discountAmount: Long? = null,

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var metadata: Map<String, Any?>? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    var customer: Customer? = null,
) : AuditablePersistable() {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    var tenant: Tenant? = null

    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = false, fetch = FetchType.LAZY)
    var redemptions: MutableList<Redemption> = mutableListOf()

    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var items: MutableList<OrderItem> = mutableListOf()
}
