package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class VoucherAssetsDto(
    @field:Schema(description = "QR code asset")
    val qr: AssetDto? = null,
    @field:Schema(description = "Barcode asset")
    val barcode: AssetDto? = null
)
