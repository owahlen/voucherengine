package org.wahlen.voucherengine.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.wahlen.voucherengine.persistence.model.customer.Customer
import java.util.UUID

interface CustomerRepository : JpaRepository<Customer, UUID> {
    fun findBySourceIdAndTenantName(sourceId: String, tenantName: String): Customer?
    fun findByIdAndTenantName(id: UUID, tenantName: String): Customer?
    fun findAllByTenantName(tenantName: String, pageable: Pageable): Page<Customer>
    fun findAllByTenantName(tenantName: String): List<Customer>
}
