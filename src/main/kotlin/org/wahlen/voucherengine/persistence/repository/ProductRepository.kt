package org.wahlen.voucherengine.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.wahlen.voucherengine.persistence.model.product.Product
import java.util.UUID

interface ProductRepository : JpaRepository<Product, UUID> {
    fun findByIdAndTenantName(id: UUID, tenantName: String): Product?
    fun findBySourceIdAndTenantName(sourceId: String, tenantName: String): Product?
    fun findAllByTenantName(tenantName: String): List<Product>
}
