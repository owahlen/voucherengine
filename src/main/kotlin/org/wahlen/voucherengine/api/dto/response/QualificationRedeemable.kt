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
    @field:Schema(description = "Validation rule id")
    val validation_rule_id: String? = null,
    @field:Schema(description = "Applicable items")
    val applicable_to: QualificationItemList? = null,
    @field:Schema(description = "Inapplicable items")
    val inapplicable_to: QualificationItemList? = null,
    @field:Schema(description = "Redeemable name")
    val name: String? = null,
    @field:Schema(description = "Campaign name")
    val campaign_name: String? = null,
    @field:Schema(description = "Campaign id")
    val campaign_id: String? = null,
    @field:Schema(description = "Redeemable metadata")
    val metadata: Map<String, Any?>? = null,
    @field:Schema(description = "Categories")
    val categories: List<CategoryResponse>? = null,
    @field:Schema(description = "Banner text")
    val banner: String? = null,
    @field:Schema(description = "Nested redeemables")
    val redeemables: List<QualificationRedeemable>? = null,
    @field:Schema(description = "Validation rules assignments")
    val validation_rules_assignments: ValidationRulesAssignmentsListResponse? = null
)
