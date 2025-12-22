package org.wahlen.voucherengine.api.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class ValidationRuleAssignmentRequest(
    @field:NotBlank
    @field:Schema(description = "Type of assignable object (voucher)", example = "voucher")
    var `object`: String? = null,
    @field:NotBlank
    @field:Schema(description = "Identifier of the assignable object", example = "code-123")
    var id: String? = null
)
