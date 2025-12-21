package org.wahlen.voucherengine.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.wahlen.voucherengine.persistence.model.voucher.Voucher
import java.util.UUID

interface VoucherRepository : JpaRepository<Voucher, UUID> {
    fun findByCode(code: String): Voucher?
}
