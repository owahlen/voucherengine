package org.wahlen.voucherengine.service.dto

data class ValidationResponse(
    val valid: Boolean,
    val voucherCode: String? = null,
    val error: ErrorResponse? = null
)

data class ErrorResponse(
    val code: String,
    val message: String
)
