package org.wahlen.voucherengine.persistence.model.tenant

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.wahlen.voucherengine.persistence.model.common.AuditablePersistable

/**
 * Represents a tenant in the multi-tenant voucherengine system.
 *
 * Tenants isolate data and operations per customer/organization.
 * All entities are scoped to a tenant, enforced via JWT claims and headers.
 */
@Entity
class Tenant(

    /**
     * Unique tenant name/identifier.
     */
    @Column(nullable = false, unique = true)
    var name: String? = null

) : AuditablePersistable()
