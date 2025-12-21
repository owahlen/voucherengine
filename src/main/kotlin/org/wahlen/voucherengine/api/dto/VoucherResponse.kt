package org.wahlen.voucherengine.api.dto

import org.wahlen.voucherengine.persistence.model.voucher.VoucherType
import java.util.UUID

data class VoucherResponse(
    val id: UUID?,
    val code: String?,
    val type: VoucherType?,
    val redemption: RedemptionDto?
)
