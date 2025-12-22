package org.wahlen.voucherengine.api.dto.request

import io.swagger.v3.oas.annotations.media.Schema

data class PublicationCampaignRequest(
    @field:Schema(description = "Campaign name or id", example = "Welcome-30D-2025-12")
    var name: String? = null,
    @field:Schema(description = "Number of vouchers to publish", example = "1")
    var count: Int? = null
)
