package org.wahlen.voucherengine.api.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class TenantCreateRequest(
    @field:NotBlank
    @field:Schema(description = "Tenant name", example = "acme")
    val name: String? = null
)
