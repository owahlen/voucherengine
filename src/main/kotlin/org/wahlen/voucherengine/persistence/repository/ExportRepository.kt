package org.wahlen.voucherengine.persistence.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.wahlen.voucherengine.persistence.model.export.Export
import java.util.UUID

interface ExportRepository : JpaRepository<Export, UUID> {
    fun findByIdAndTenantName(id: UUID, tenantName: String): Export?
    fun findAllByTenantName(tenantName: String, pageable: Pageable): Page<Export>
    fun findAllByTenantName(tenantName: String): List<Export>
}
