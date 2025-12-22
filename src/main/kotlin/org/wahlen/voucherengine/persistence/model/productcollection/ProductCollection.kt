package org.wahlen.voucherengine.persistence.model.productcollection

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
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

@Entity
@Table(
    name = "product_collection",
    uniqueConstraints = [
        UniqueConstraint(name = "uc_product_collection_tenant_name", columnNames = ["tenant_id", "name"])
    ]
)
class ProductCollection(
    @Column
    var name: String? = null,

    @Enumerated(EnumType.STRING)
    @Column
    var type: ProductCollectionType? = null,

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var filter: Map<String, Any?>? = null
) : AuditablePersistable() {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    var tenant: Tenant? = null

    @OneToMany(mappedBy = "collection", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var items: MutableList<ProductCollectionItem> = mutableListOf()
}
