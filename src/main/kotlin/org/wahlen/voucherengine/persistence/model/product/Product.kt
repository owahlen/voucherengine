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
 * Represents a product in the catalog used for validation rules and order item references.
 *
 * Products group SKUs, define pricing, and provide metadata for eligibility checks
 * in validation rules (e.g., product inclusion/exclusion, applicable_to logic).
 *
 * Voucherengine API Docs: Products.
 */
@Entity
@Table(
    name = "product",
    uniqueConstraints = [
        UniqueConstraint(name = "uc_product_tenant_source", columnNames = ["tenant_id", "source_id"])
    ]
)
class Product(

    /**
     * External source identifier for this product.
     */
    @Column(name = "source_id")
    var sourceId: String? = null,

    /**
     * Product name.
     */
    @Column
    var name: String? = null,

    /**
     * Price in smallest currency unit (e.g., cents).
     */
    @Column
    var price: Long? = null,

    /**
     * Product attributes (tags, categories, etc.) as JSON array.
     */
    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var attributes: List<String>? = null,

    /**
     * The metadata object stores all custom attributes assigned to the product.
     */
    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var metadata: Map<String, Any?>? = null,

    /**
     * Product image URL.
     */
    @Column(name = "image_url", columnDefinition = "TEXT")
    var imageUrl: String? = null,

    /**
     * Tenant that owns this product.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    var tenant: Tenant? = null,

    /**
     * SKUs (variants) for this product.
     */
    @OneToMany(mappedBy = "product", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var skus: MutableList<Sku> = mutableListOf()

) : AuditablePersistable()
