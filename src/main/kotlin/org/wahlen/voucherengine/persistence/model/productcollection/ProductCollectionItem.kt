package org.wahlen.voucherengine.persistence.model.productcollection

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.wahlen.voucherengine.persistence.model.common.AuditablePersistable
import org.wahlen.voucherengine.persistence.model.tenant.Tenant

@Entity
@Table(name = "product_collection_item")
class ProductCollectionItem(
    @Column(name = "item_id")
    var itemId: String? = null,

    @Column(name = "product_id")
    var productId: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "object_type")
    var objectType: ProductCollectionItemType? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_collection_id", nullable = false)
    var collection: ProductCollection? = null
) : AuditablePersistable() {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    var tenant: Tenant? = null
}
