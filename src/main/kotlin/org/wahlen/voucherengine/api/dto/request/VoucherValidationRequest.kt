package org.wahlen.voucherengine.api.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid

data class VoucherValidationRequest(
    @field:Valid
    @field:Schema(description = "Customer context used during validation")
    var customer: CustomerReferenceDto? = null,
    @field:Valid
    @field:Schema(description = "Order context used during validation")
    var order: OrderRequest? = null,
    @field:Schema(description = "Categories related to this validation", example = """["11111111-1111-1111-1111-111111111111"]""")
    var categories: List<java.util.UUID>? = null,
    @field:Schema(description = "Redemption metadata for rule checks", example = """{"channel":"web"}""")
    var metadata: Map<String, Any?>? = null
)
