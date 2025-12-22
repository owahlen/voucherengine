package org.wahlen.voucherengine.api.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty

data class RedemptionRequest(
    @field:NotEmpty
    @field:Valid
    @field:Schema(description = "Redeemables to process", required = true)
    var redeemables: List<RedeemableDto>,
    @field:Valid
    @field:Schema(description = "Customer context for the redemption")
    var customer: CustomerReferenceDto? = null,
    @field:Valid
    @field:Schema(description = "Order context for the redemption")
    var order: OrderRequest? = null
)
