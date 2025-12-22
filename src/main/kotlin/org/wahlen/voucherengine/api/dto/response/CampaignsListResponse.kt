package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class CampaignsListResponse(
    @field:Schema(description = "Object marker", example = "list")
    val `object`: String = "list",
    @field:Schema(description = "Data reference", example = "campaigns")
    val data_ref: String = "campaigns",
    @field:Schema(description = "Campaigns list")
    val campaigns: List<CampaignResponse> = emptyList(),
    @field:Schema(description = "Total number of campaigns")
    val total: Int = 0
)
