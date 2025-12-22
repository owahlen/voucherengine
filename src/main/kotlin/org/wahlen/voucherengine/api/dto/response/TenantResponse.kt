package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

data class TenantResponse(
    @field:Schema(description = "Tenant id")
    val id: UUID?,
    @field:Schema(description = "Tenant name", example = "acme")
    val name: String?,
    @field:Schema(description = "Creation timestamp", example = "2025-01-12T09:12:45.382Z")
    val created_at: Instant? = null,
    @field:Schema(description = "Update timestamp", example = "2025-01-12T09:12:45.382Z")
    val updated_at: Instant? = null
)
