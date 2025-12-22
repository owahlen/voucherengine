package org.wahlen.voucherengine.api.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class CustomerReferenceDto(
    @field:NotBlank
    @field:Schema(description = "Client-provided stable identifier", example = "customer-123")
    var source_id: String? = null,
    @field:Email
    @field:Schema(description = "Customer email", example = "customer@example.com")
    var email: String? = null,
    @field:Schema(description = "Customer name", example = "Ada Lovelace")
    var name: String? = null,
    @field:Schema(description = "Phone number", example = "+1555123456")
    var phone: String? = null
)
