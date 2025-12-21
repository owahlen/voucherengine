package org.wahlen.voucherengine.api.dto.request

import jakarta.validation.Valid

data class VoucherValidationRequest(
    @field:Valid
    var customer: CustomerReferenceDto? = null,
    @field:Valid
    var order: OrderRequest? = null
)
