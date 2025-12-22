package org.wahlen.voucherengine.persistence.model.product

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.wahlen.voucherengine.persistence.model.common.AuditablePersistable
import org.wahlen.voucherengine.persistence.model.tenant.Tenant

/**
 * Represents a product in the catalog. Products are long-lived and referenced by orders and rules.
 */
@Entity
@Table(
    name = "product",
    uniqueConstraints = [
        UniqueConstraint(name = "uc_product_tenant_source", columnNames = ["tenant_id", "source_id"])
    ]
)
class Product(
    @Column(name = "source_id")
    var sourceId: String? = null,

    @Column
    var name: String? = null,

    @Column
    var price: Long? = null,

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var attributes: List<String>? = null,

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var metadata: Map<String, Any?>? = null,

    @Column(name = "image_url", columnDefinition = "TEXT")
    var imageUrl: String? = null
) : AuditablePersistable() {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    var tenant: Tenant? = null

    @OneToMany(mappedBy = "product", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var skus: MutableList<Sku> = mutableListOf()
}
