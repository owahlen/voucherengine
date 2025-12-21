package org.wahlen.voucherengine.api.dto.request

import jakarta.validation.constraints.NotBlank

data class ValidationRuleAssignmentRequest(
    @field:NotBlank
    val `object`: String? = null,
    @field:NotBlank
    val id: String? = null
)
