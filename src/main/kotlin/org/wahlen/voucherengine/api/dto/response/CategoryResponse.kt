package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

data class CategoryResponse(
    @field:Schema(description = "Category id", example = "c_123")
    val id: UUID?,
    @field:Schema(description = "Category name", example = "electronics")
    val name: String?,
    @field:Schema(description = "Creation timestamp")
    val created_at: Instant? = null,
    @field:Schema(description = "Update timestamp")
    val updated_at: Instant? = null
)
