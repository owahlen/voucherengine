package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

data class QualificationRedeemable(
    @field:Schema(description = "Redeemable id", example = "WELCOME10")
    val id: String? = null,
    @field:Schema(description = "Redeemable object type", example = "voucher")
    val `object`: String? = null,
    @field:Schema(description = "Created at")
    val created_at: Instant? = null,
    @field:Schema(description = "Result details")
    val result: QualificationRedeemableResult? = null,
    @field:Schema(description = "Order summary")
    val order: QualificationOrderSummary? = null,
    @field:Schema(description = "Redeemable name")
    val name: String? = null,
    @field:Schema(description = "Campaign name")
    val campaign_name: String? = null,
    @field:Schema(description = "Campaign id")
    val campaign_id: String? = null,
    @field:Schema(description = "Redeemable metadata")
    val metadata: Map<String, Any?>? = null,
    @field:Schema(description = "Categories")
    val categories: List<CategoryResponse>? = null
)
