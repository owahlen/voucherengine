package org.wahlen.voucherengine.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.wahlen.voucherengine.persistence.model.redemption.Redemption
import java.util.UUID

interface RedemptionRepository : JpaRepository<Redemption, UUID> {
    fun countByVoucherId(voucherId: UUID): Long
    fun countByVoucherIdAndCustomerId(voucherId: UUID, customerId: UUID): Long
}
