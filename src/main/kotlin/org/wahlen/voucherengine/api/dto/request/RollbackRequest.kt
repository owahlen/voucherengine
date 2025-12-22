package org.wahlen.voucherengine.api.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class RollbackRequest(
    @field:NotBlank
    @field:Schema(description = "Reason for rollback", example = "Order canceled")
    var reason: String? = null,
    @field:Schema(description = "Rollback amount in minor units", example = "500")
    var amount: Long? = null,
    @field:Schema(description = "Optional metadata to store with the rollback")
    var metadata: Map<String, Any?>? = null
)
