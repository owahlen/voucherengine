package org.wahlen.voucherengine.api.dto.common

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Scope for amount-off discounts")
enum class AmountOffType {
    FIXED
}
