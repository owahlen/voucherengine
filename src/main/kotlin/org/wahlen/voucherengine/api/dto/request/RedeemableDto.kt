package org.wahlen.voucherengine.api.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class RedeemableDto(
    @field:NotBlank
    @field:Schema(description = "Type of redeemable (voucher only supported)", example = "voucher")
    var `object`: String,
    @field:NotBlank
    @field:Schema(description = "Identifier (voucher code)", example = "SUMMER-10")
    var id: String
)
