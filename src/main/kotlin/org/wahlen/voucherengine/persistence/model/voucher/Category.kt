package org.wahlen.voucherengine.persistence.model.voucher

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.wahlen.voucherengine.persistence.model.common.AuditablePersistable
import org.wahlen.voucherengine.persistence.model.tenant.Tenant

/**
 * Represents a Voucherengine category used to group vouchers for targeting and reporting.
 *
 * Categories allow organizing vouchers into hierarchical taxonomies that can be used
 * for filtering, analytics, or validation rule targeting.
 *
 * Voucherengine API Docs: Categories.
 */
@Entity
class Category(


    @Column
    var name: String? = null,
) : AuditablePersistable() {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    var tenant: Tenant? = null

    @ManyToMany(mappedBy = "categories", fetch = FetchType.LAZY)
    var vouchers: MutableSet<Voucher> = mutableSetOf()
}
