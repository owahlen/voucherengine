package org.wahlen.voucherengine.api.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class ExportCreateRequest(
    @field:NotBlank
    @field:Schema(description = "Type of object to export", example = "voucher")
    val exported_object: String? = null,
    @field:Schema(description = "Export parameters")
    val parameters: ExportParameters? = null
)
