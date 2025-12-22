package org.wahlen.voucherengine.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.wahlen.voucherengine.persistence.model.tenant.Tenant
import java.util.UUID

interface TenantRepository : JpaRepository<Tenant, UUID> {
    fun findByName(name: String): Tenant?
}
