package org.wahlen.voucherengine.api.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import org.wahlen.voucherengine.api.dto.common.SessionDto

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
    var order: OrderRequest? = null,
    @field:Schema(description = "Tracking id", example = "track_123")
    var tracking_id: String? = null,
    @field:Schema(description = "Redemption metadata for rule checks")
    var metadata: Map<String, Any?>? = null,
    @field:Valid
    @field:Schema(description = "Session settings")
    var session: SessionDto? = null,
    @field:Valid
    @field:Schema(description = "Validation options")
    var options: ValidationOptions? = null
)
