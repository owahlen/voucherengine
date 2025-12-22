package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.wahlen.voucherengine.persistence.model.campaign.CampaignMode
import org.wahlen.voucherengine.persistence.model.campaign.CampaignType
import java.time.Instant
import java.util.UUID

data class CampaignResponse(
    @field:Schema(description = "Campaign id", example = "c_123")
    val id: UUID?,
    @field:Schema(description = "Object marker", example = "campaign")
    val `object`: String = "campaign",
    @field:Schema(description = "Campaign name", example = "Black Friday 2026")
    val name: String?,
    @field:Schema(description = "Campaign type", example = "DISCOUNT_COUPONS")
    val type: CampaignType? = null,
    @field:Schema(description = "Campaign mode", example = "INTEGRATION")
    val mode: CampaignMode? = null,
    @field:Schema(description = "Code pattern for vouchers issued under this campaign", example = "BF2026-####")
    val code_pattern: String? = null,
    @field:Schema(description = "Start date", example = "2025-11-20T00:00:00Z")
    val start_date: Instant? = null,
    @field:Schema(description = "Expiration date", example = "2025-12-01T00:00:00Z")
    val expiration_date: Instant? = null,
    @field:Schema(description = "Metadata payload")
    val metadata: Map<String, Any?>? = null,
    @field:Schema(description = "Active flag", example = "true")
    val active: Boolean? = null,
    @field:Schema(description = "Campaign description", example = "Black Friday discounts")
    val description: String? = null,
    @field:Schema(description = "Creation timestamp", example = "2025-01-12T09:12:45.382Z")
    val created_at: Instant? = null,
    @field:Schema(description = "Update timestamp", example = "2025-01-12T09:12:45.382Z")
    val updated_at: Instant? = null,
)
