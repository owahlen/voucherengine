package org.wahlen.voucherengine.api.dto.request

import jakarta.validation.constraints.NotBlank

data class ValidationRuleAssignmentRequest(
    @field:NotBlank
    var `object`: String? = null,
    @field:NotBlank
    var id: String? = null
)
