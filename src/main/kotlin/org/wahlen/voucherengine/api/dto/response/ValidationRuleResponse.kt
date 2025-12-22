package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

data class ValidationRuleResponse(
    @field:Schema(description = "Validation rule id", example = "val_0f8a1c2b")
    val id: UUID?,
    @field:Schema(description = "Object type marker", example = "validation_rule")
    val `object`: String = "validation_rule",
    @field:Schema(description = "Rule name", example = "One redemption per customer")
    val name: String?,
    @field:Schema(description = "Rule type", example = "redemptions")
    val type: String?,
    @field:Schema(description = "Rule context type", example = "voucher.discount_voucher")
    val context_type: String? = null,
    @field:Schema(description = "Rule conditions payload (full rule set)")
    val conditions: Map<String, Any?>?,
    @field:Schema(description = "Logic expression combining rules, if present", example = "1 and 2")
    val logic: String? = null,
    @field:Schema(description = "Bundle rules payload")
    val bundle_rules: Map<String, Any?>? = null,
    @field:Schema(description = "Applicable to payload")
    val applicable_to: Map<String, Any?>? = null,
    @field:Schema(description = "Error payload with code/message")
    val error: Map<String, Any?>? = null,
    @field:Schema(description = "Created timestamp", example = "2025-01-12T09:31:00Z")
    val created_at: Instant?
)
