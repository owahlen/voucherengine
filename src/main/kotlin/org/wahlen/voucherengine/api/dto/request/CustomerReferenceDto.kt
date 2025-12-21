package org.wahlen.voucherengine.api.dto.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class CustomerReferenceDto(
    @field:NotBlank
    val source_id: String? = null,
    @field:Email
    val email: String? = null,
    val name: String? = null,
    val phone: String? = null
)
