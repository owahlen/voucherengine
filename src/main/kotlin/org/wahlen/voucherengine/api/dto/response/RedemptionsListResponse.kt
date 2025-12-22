package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class RedemptionsListResponse(
    @field:Schema(description = "Object marker", example = "list")
    val `object`: String = "list",
    @field:Schema(description = "Data reference", example = "redemptions")
    val data_ref: String = "redemptions",
    @field:Schema(description = "Redemptions list")
    val redemptions: List<RedemptionDetailResponse> = emptyList(),
    @field:Schema(description = "Total number of redemptions")
    val total: Int = 0
)
