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

/**
 * Represents an item within a static product collection.
 *
 * Links products or SKUs to a collection for validation rule targeting.
 * Dynamic collections use filters instead of explicit items.
 *
 * Voucherengine API Docs: Product Collections.
 */
@Entity
@Table(name = "product_collection_item")
class ProductCollectionItem(

    /**
     * Item ID (product or SKU ID).
     */
    @Column(name = "item_id")
    var itemId: String? = null,

    /**
     * Product ID if this item represents a product.
     */
    @Column(name = "product_id")
    var productId: String? = null,

    /**
     * Type of object (PRODUCT or SKU).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "object_type")
    var objectType: ProductCollectionItemType? = null,

    /**
     * Collection this item belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_collection_id", nullable = false)
    var collection: ProductCollection? = null,

    /**
     * Tenant that owns this collection item.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    var tenant: Tenant? = null

) : AuditablePersistable()
