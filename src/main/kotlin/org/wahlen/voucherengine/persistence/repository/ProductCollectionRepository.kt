package org.wahlen.voucherengine.persistence.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.wahlen.voucherengine.persistence.model.productcollection.ProductCollection
import java.util.UUID

interface ProductCollectionRepository : JpaRepository<ProductCollection, UUID> {
    fun findByIdAndTenantName(id: UUID, tenantName: String): ProductCollection?
    fun findByNameAndTenantName(name: String, tenantName: String): ProductCollection?
    fun findAllByTenantName(tenantName: String, pageable: Pageable): Page<ProductCollection>
    fun findAllByTenantName(tenantName: String): List<ProductCollection>
}
