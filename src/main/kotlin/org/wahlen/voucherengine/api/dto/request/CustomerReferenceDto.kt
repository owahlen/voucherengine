package org.wahlen.voucherengine.api.dto.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class CustomerReferenceDto(
    @field:NotBlank
    var source_id: String? = null,
    @field:Email
    var email: String? = null,
    var name: String? = null,
    var phone: String? = null
)
