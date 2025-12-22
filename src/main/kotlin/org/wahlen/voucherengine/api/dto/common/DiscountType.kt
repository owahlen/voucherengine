package org.wahlen.voucherengine.api.dto.common

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Supported discount types")
enum class DiscountType {
    PERCENT,
    AMOUNT,
    UNIT,
    FIXED
}
