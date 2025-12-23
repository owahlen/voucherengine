package org.wahlen.voucherengine.persistence.model.tenant

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.wahlen.voucherengine.persistence.model.common.AuditablePersistable

/**
 * Represents a tenant owning data in the system.
 */
@Entity
class Tenant(
    @Column(nullable = false, unique = true)
    var name: String? = null
) : AuditablePersistable()
