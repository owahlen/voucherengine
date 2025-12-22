package org.wahlen.voucherengine.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.persistence.model.redemption.Redemption
import org.wahlen.voucherengine.persistence.repository.RedemptionRepository
import java.util.UUID

@Service
class RedemptionService(
    private val redemptionRepository: RedemptionRepository
) {

    @Transactional(readOnly = true)
    fun list(tenantName: String): List<Redemption> =
        redemptionRepository.findAllByTenantName(tenantName)

    @Transactional(readOnly = true)
    fun get(tenantName: String, id: UUID): Redemption? =
        redemptionRepository.findByIdAndTenantName(id, tenantName)
}
