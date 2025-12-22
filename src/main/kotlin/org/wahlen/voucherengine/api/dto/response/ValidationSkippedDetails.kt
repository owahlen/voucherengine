package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class ValidationSkippedDetails(
    @field:Schema(description = "Skip reason key", example = "preceding_validation_failed")
    val key: String? = null,
    @field:Schema(description = "Skip reason message", example = "Redeemable cannot be applied due to preceding validation failure")
    val message: String? = null
)
