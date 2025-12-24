package org.wahlen.voucherengine.persistence.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.wahlen.voucherengine.persistence.model.voucher.VoucherTransaction
import java.util.UUID

interface VoucherTransactionRepository : JpaRepository<VoucherTransaction, UUID> {
    fun findAllByVoucher_IdAndTenant_Name(voucherId: UUID, tenantName: String, pageable: Pageable): Page<VoucherTransaction>
    fun findAllByVoucher_IdInAndTenant_Name(voucherIds: List<UUID>, tenantName: String, pageable: Pageable): Page<VoucherTransaction>
}
