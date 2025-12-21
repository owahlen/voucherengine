package org.wahlen.voucherengine.api.dto.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class CustomerCreateRequest(
    @field:NotBlank
    var source_id: String? = null,
    @field:Email
    var email: String? = null,
    var name: String? = null,
    var phone: String? = null,
    var metadata: Map<String, Any?>? = null,
)
