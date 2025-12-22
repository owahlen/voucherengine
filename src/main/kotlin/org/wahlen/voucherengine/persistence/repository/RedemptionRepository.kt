package org.wahlen.voucherengine.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.wahlen.voucherengine.persistence.model.redemption.Redemption
import java.util.UUID

interface RedemptionRepository : JpaRepository<Redemption, UUID> {
    fun countByVoucherIdAndTenantName(voucherId: UUID, tenantName: String): Long
    fun countByVoucherIdAndCustomerIdAndTenantName(voucherId: UUID, customerId: UUID, tenantName: String): Long
    fun findAllByTenantName(tenantName: String): List<Redemption>
    fun findByIdAndTenantName(id: UUID, tenantName: String): Redemption?
    fun findAllByTenantNameAndVoucherId(tenantName: String, voucherId: UUID): List<Redemption>
    fun findAllByTenantNameAndVoucherIdIn(tenantName: String, voucherIds: List<UUID>): List<Redemption>
}
