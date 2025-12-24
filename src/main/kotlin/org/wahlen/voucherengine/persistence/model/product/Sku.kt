package org.wahlen.voucherengine.persistence.model.product

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.wahlen.voucherengine.persistence.model.common.AuditablePersistable
import org.wahlen.voucherengine.persistence.model.tenant.Tenant

/**
 * Represents a SKU (stock-keeping unit or product variant).
 *
 * SKUs define specific variants of a product (e.g., size, color) with their own
 * pricing, attributes, and metadata. Referenced by order items and validation rules.
 *
 * Voucherengine API Docs: Products → SKUs.
 */
@Entity
@Table(
    name = "sku",
    uniqueConstraints = [
        UniqueConstraint(name = "uc_sku_tenant_source", columnNames = ["tenant_id", "source_id"])
    ]
)
class Sku(

    /**
     * External source identifier for this SKU.
     */
    @Column(name = "source_id")
    var sourceId: String? = null,

    /**
     * SKU code/identifier.
     */
    @Column
    var sku: String? = null,

    /**
     * Price in smallest currency unit (e.g., cents).
     */
    @Column
    var price: Long? = null,

    /**
     * Currency code (e.g., USD, EUR).
     */
    @Column
    var currency: String? = null,

    /**
     * SKU attributes (e.g., size, color, material).
     */
    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var attributes: Map<String, Any?>? = null,

    /**
     * The metadata object stores all custom attributes assigned to the SKU.
     */
    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var metadata: Map<String, Any?>? = null,

    /**
     * SKU image URL.
     */
    @Column(name = "image_url", columnDefinition = "TEXT")
    var imageUrl: String? = null,

    /**
     * Product this SKU belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    var product: Product? = null,

    /**
     * Tenant that owns this SKU.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    var tenant: Tenant? = null

) : AuditablePersistable()
