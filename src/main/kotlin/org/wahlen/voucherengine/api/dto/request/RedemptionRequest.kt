package org.wahlen.voucherengine.api.dto.request

import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty

data class RedemptionRequest(
    @field:NotEmpty
    @field:Valid
    val redeemables: List<RedeemableDto>,
    @field:Valid
    val customer: CustomerReferenceDto? = null,
    @field:Valid
    val order: OrderRequest? = null
)
