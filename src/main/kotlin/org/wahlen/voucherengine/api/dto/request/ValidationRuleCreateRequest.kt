package org.wahlen.voucherengine.api.dto.request

import jakarta.validation.constraints.NotBlank

data class ValidationRuleCreateRequest(
    @field:NotBlank
    val name: String? = null,
    @field:NotBlank
    val type: String? = null,
    val conditions: Map<String, Any?>? = null
)
