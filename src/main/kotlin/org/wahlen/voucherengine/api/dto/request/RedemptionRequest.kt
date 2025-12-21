package org.wahlen.voucherengine.api.dto.request

import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty

data class RedemptionRequest(
    @field:NotEmpty
    @field:Valid
    var redeemables: List<RedeemableDto>,
    @field:Valid
    var customer: CustomerReferenceDto? = null,
    @field:Valid
    var order: OrderRequest? = null
)
