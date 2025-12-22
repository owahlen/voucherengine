package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class QualificationOrderSummary(
    @field:Schema(description = "Order amount")
    val amount: Long? = null,
    @field:Schema(description = "Total amount after discounts")
    val total_amount: Long? = null
)
