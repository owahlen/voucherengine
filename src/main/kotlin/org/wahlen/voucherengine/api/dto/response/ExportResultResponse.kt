package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class ExportResultResponse(
    @field:Schema(description = "URL of the CSV file")
    val url: String? = null
)
