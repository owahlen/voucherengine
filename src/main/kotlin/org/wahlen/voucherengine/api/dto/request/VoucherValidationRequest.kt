package org.wahlen.voucherengine.api.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid

data class VoucherValidationRequest(
    @field:Valid
    @field:Schema(description = "Customer context used during validation")
    var customer: CustomerReferenceDto? = null,
    @field:Valid
    @field:Schema(description = "Order context used during validation")
    var order: OrderRequest? = null
)
