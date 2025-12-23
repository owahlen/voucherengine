package org.wahlen.voucherengine.api.dto.response

import org.wahlen.voucherengine.api.dto.common.DiscountDto

data class ValidationResponse(
    val valid: Boolean,
    val voucher: VoucherResponse? = null,
    val discount: DiscountDto? = null,
    val order: ValidationOrderSummary? = null,
    val error: ErrorResponse? = null,
    val validationRuleId: String? = null
)

data class ValidationOrderSummary(
    val amount: Long? = null,
    val discount_amount: Long? = null,
    val total_amount: Long? = null
)

data class ErrorResponse(
    val code: String,
    val message: String
)
