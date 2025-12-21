package org.wahlen.voucherengine.api.dto.request

import jakarta.validation.constraints.NotBlank

data class ValidationRuleCreateRequest(
    @field:NotBlank
    var name: String? = null,
    @field:NotBlank
    var type: String? = null,
    var conditions: Map<String, Any?>? = null
)
