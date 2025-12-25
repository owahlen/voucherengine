package org.wahlen.voucherengine.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.wahlen.voucherengine.persistence.model.order.Order
import java.util.UUID

interface OrderRepository : JpaRepository<Order, UUID> {
    fun findBySourceIdAndTenantName(sourceId: String, tenantName: String): Order?
    fun findByIdAndTenantName(id: UUID, tenantName: String): Order?
    fun findAllByTenantName(tenantName: String, pageable: Pageable): Page<Order>
    fun findAllByTenantName(tenantName: String): List<Order>
    fun countByTenantName(tenantName: String): Long
}
