package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class QualificationErrorResult(
    @field:Schema(description = "Error code")
    val code: String? = null,
    @field:Schema(description = "Error message")
    val message: String? = null
)
