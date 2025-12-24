package org.wahlen.voucherengine.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.wahlen.voucherengine.persistence.model.segment.Segment
import java.util.UUID

interface SegmentRepository : JpaRepository<Segment, UUID> {
    
    fun findAllByTenant_Name(tenantName: String): List<Segment>
    
    fun findByIdAndTenant_Name(id: UUID, tenantName: String): Segment?
    
    fun findAllByTenant_NameAndCustomerIdsContaining(tenantName: String, customerId: UUID): List<Segment>
}
