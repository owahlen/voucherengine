package org.wahlen.voucherengine.api.dto.request

import jakarta.validation.constraints.NotNull

data class VoucherBalanceUpdateRequest(
    @field:NotNull(message = "amount is required")
    var amount: Long? = null,

    var source_id: String? = null,

    var reason: String? = null
)
