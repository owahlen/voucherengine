package org.wahlen.voucherengine.api.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid

data class QualificationRequest(
    @field:Valid
    @field:Schema(description = "Customer context")
    var customer: CustomerReferenceDto? = null,
    @field:Valid
    @field:Schema(description = "Order context")
    var order: OrderRequest? = null,
    @field:Schema(description = "Tracking id", example = "cust-123")
    var tracking_id: String? = null,
    @field:Schema(description = "Qualification scenario", example = "ALL")
    var scenario: String? = null,
    @field:Valid
    @field:Schema(description = "Qualification options")
    var options: QualificationOptions? = null,
    @field:Schema(description = "Session settings")
    var session: QualificationSession? = null,
    @field:Schema(description = "Redemption metadata for rule checks")
    var metadata: Map<String, Any?>? = null
)
