package org.wahlen.voucherengine.api.dto.request

import jakarta.validation.constraints.NotBlank

data class RedeemableDto(
    @field:NotBlank
    val `object`: String,
    @field:NotBlank
    val id: String
)
