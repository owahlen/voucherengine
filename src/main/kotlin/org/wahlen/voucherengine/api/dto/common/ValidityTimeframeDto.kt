package org.wahlen.voucherengine.api.dto.common

import io.swagger.v3.oas.annotations.media.Schema

data class ValidityTimeframeDto(
    @field:Schema(description = "ISO 8601 duration the voucher stays active in each interval", example = "PT1H")
    var duration: String? = null,
    @field:Schema(description = "ISO 8601 interval between activations", example = "P2D")
    var interval: String? = null
)
