package org.wahlen.voucherengine.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.wahlen.voucherengine.persistence.model.product.Sku
import java.util.UUID

interface SkuRepository : JpaRepository<Sku, UUID> {
    fun findByIdAndTenantName(id: UUID, tenantName: String): Sku?
    fun findBySourceIdAndTenantName(sourceId: String, tenantName: String): Sku?
    fun findAllByTenantName(tenantName: String): List<Sku>
    fun findAllByProductIdAndTenantName(productId: UUID, tenantName: String): List<Sku>
}
