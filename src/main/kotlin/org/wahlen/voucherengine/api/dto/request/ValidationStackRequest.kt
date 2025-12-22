package org.wahlen.voucherengine.api.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty

data class ValidationStackRequest(
    @field:NotEmpty
    @field:Valid
    @field:Schema(description = "Redeemables to validate together", required = true)
    var redeemables: List<RedeemableDto>,
    @field:Valid
    @field:Schema(description = "Customer context for validation")
    var customer: CustomerReferenceDto? = null,
    @field:Valid
    @field:Schema(description = "Order context for validation")
    var order: OrderRequest? = null
)
