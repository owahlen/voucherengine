package org.wahlen.voucherengine.persistence.model.voucher

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToMany
import jakarta.persistence.Table
import org.wahlen.voucherengine.persistence.model.common.AuditablePersistable

/**
 * Represents a Voucherengine category used to group vouchers for targeting and reporting.
 *
 * Categories allow organizing vouchers into hierarchical taxonomies that can be used
 * for filtering, analytics, or validation rule targeting.
 *
 * Voucherengine API Docs: Categories.
 */
@Entity
@Table
class Category(


    @Column
    var name: String? = null,
    @Column
    var hierarchy: Int? = null,
) : AuditablePersistable() {
    @ManyToMany(mappedBy = "categories", fetch = FetchType.LAZY)
    var vouchers: MutableSet<Voucher> = mutableSetOf()
}
