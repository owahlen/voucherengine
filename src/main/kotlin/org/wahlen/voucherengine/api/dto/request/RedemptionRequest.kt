package org.wahlen.voucherengine.api.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import org.wahlen.voucherengine.api.dto.common.SessionDto

data class RedemptionRequest(
    @field:NotEmpty
    @field:Valid
    @field:Schema(description = "Redeemables to process", required = true)
    var redeemables: List<RedeemableDto>,
    @field:Valid
    @field:Schema(description = "Customer context for the redemption")
    var customer: CustomerReferenceDto? = null,
    @field:Valid
    @field:Schema(description = "Order context for the redemption")
    var order: OrderRequest? = null,
    @field:Schema(description = "Tracking id", example = "track_123")
    var tracking_id: String? = null,
    @field:Schema(description = "Redemption metadata")
    var metadata: Map<String, Any?>? = null,
    @field:Valid
    @field:Schema(description = "Session settings")
    var session: SessionDto? = null
)
