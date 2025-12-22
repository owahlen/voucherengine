package org.wahlen.voucherengine.api.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class CategoryCreateRequest(
    @field:NotBlank
    @field:Schema(description = "Category name", example = "electronics")
    var name: String? = null
)
