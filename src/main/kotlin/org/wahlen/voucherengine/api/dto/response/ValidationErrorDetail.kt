package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class ValidationErrorDetail(
    @field:Schema(description = "Error code", example = "voucher_not_found")
    val code: String? = null,
    @field:Schema(description = "Error message", example = "Voucher does not exist.")
    val message: String? = null
)
