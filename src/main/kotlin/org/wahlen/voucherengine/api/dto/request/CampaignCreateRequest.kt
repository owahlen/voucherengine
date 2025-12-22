package org.wahlen.voucherengine.api.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import org.wahlen.voucherengine.persistence.model.campaign.CampaignMode
import org.wahlen.voucherengine.persistence.model.campaign.CampaignType

data class CampaignCreateRequest(
    @field:NotBlank
    @field:Schema(description = "Campaign name", example = "Black Friday 2026")
    var name: String? = null,
    @field:Schema(description = "Campaign type", example = "DISCOUNT_COUPONS")
    var type: CampaignType? = null,
    @field:Schema(description = "Campaign mode", example = "INTEGRATION")
    var mode: CampaignMode? = null,
    @field:Schema(description = "Code pattern for vouchers issued under this campaign", example = "BF2026-####")
    var code_pattern: String? = null,
    @field:Schema(description = "Start date in ISO-8601", example = "2025-11-20T00:00:00Z")
    var start_date: java.time.Instant? = null,
    @field:Schema(description = "Expiration date in ISO-8601", example = "2025-12-01T00:00:00Z")
    var expiration_date: java.time.Instant? = null,
    @field:Schema(description = "Arbitrary campaign metadata", example = """{"channel":"email"}""")
    var metadata: Map<String, Any?>? = null,
    @field:Schema(description = "Explicit active flag", example = "true")
    var active: Boolean? = null,
    @field:Schema(description = "Campaign description", example = "Black Friday discounts")
    var description: String? = null
)
