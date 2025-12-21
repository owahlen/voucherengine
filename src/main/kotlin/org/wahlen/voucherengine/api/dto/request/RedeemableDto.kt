package org.wahlen.voucherengine.api.dto.request

import jakarta.validation.constraints.NotBlank

data class RedeemableDto(
    @field:NotBlank
    var `object`: String,
    @field:NotBlank
    var id: String
)
