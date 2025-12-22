package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class AssetDto(
    @field:Schema(description = "Asset identifier", example = "qr_123")
    val id: String? = null,
    @field:Schema(description = "Public URL to the asset", example = "https://example.com/qr.png")
    val url: String? = null
)
