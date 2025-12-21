package org.wahlen.voucherengine.api.dto.request

import jakarta.validation.Valid

data class VoucherValidationRequest(
    @field:Valid
    val customer: CustomerReferenceDto? = null,
    @field:Valid
    val order: OrderRequest? = null
)
