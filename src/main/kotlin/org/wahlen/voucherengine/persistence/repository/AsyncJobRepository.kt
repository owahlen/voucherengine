package org.wahlen.voucherengine.persistence.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.wahlen.voucherengine.persistence.model.async.AsyncJob
import org.wahlen.voucherengine.persistence.model.async.AsyncJobStatus
import java.util.UUID

interface AsyncJobRepository : JpaRepository<AsyncJob, UUID> {
    fun findByIdAndTenant_Name(id: UUID, tenantName: String): AsyncJob?
    fun findAllByTenant_NameAndStatusOrderByCreatedAtDesc(
        tenantName: String,
        status: AsyncJobStatus
    ): List<AsyncJob>
    fun findAllByTenant_Name(tenantName: String, pageable: Pageable): Page<AsyncJob>
}
