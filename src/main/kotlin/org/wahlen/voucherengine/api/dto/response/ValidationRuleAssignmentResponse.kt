package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

data class ValidationRuleAssignmentResponse(
    @field:Schema(description = "Assignment id", example = "asgm_LnY1g7UNFA9KyDrD")
    val id: UUID?,
    @field:Schema(description = "Validation rule id", example = "val_0f8a1c2b")
    val rule_id: UUID?,
    @field:Schema(description = "Related object id (voucher code or id)", example = "SUMMER2026")
    val related_object_id: String?,
    @field:Schema(description = "Related object type", example = "voucher")
    val related_object_type: String?,
    @field:Schema(description = "Object type marker", example = "validation_rules_assignment")
    val `object`: String = "validation_rules_assignment",
    @field:Schema(description = "Validation status for the assignment", example = "VALID")
    val validation_status: String? = null,
    @field:Schema(description = "Omitted rules identifiers, if any")
    val validation_omitted_rules: Set<String> = emptySet(),
    @field:Schema(description = "Creation timestamp", example = "2025-01-12T09:31:00Z")
    val created_at: Instant? = null,
    @field:Schema(description = "Updated timestamp", example = "2025-01-12T09:31:00Z")
    val updated_at: Instant? = null,
)
