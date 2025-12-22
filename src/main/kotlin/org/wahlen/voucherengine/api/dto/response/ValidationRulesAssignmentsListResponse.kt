package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class ValidationRulesAssignmentsListResponse(
    @field:Schema(description = "Object marker", example = "list")
    val `object`: String = "list",
    @field:Schema(description = "Data reference", example = "data")
    val data_ref: String = "data",
    @field:Schema(description = "Assignments list")
    val data: List<ValidationRuleAssignmentResponse> = emptyList(),
    @field:Schema(description = "Total assignments")
    val total: Int = 0
)
