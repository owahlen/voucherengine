package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.wahlen.voucherengine.api.dto.common.DiscountDto
import org.wahlen.voucherengine.api.dto.common.GiftDto
import org.wahlen.voucherengine.api.dto.common.LoyaltyCardDto
import org.wahlen.voucherengine.api.dto.common.RedemptionDto
import org.wahlen.voucherengine.api.dto.common.ValidityHoursDto
import org.wahlen.voucherengine.api.dto.common.ValidityTimeframeDto
import org.wahlen.voucherengine.persistence.model.voucher.VoucherType
import java.time.Instant
import java.util.UUID

data class VoucherResponse(
    @field:Schema(description = "Voucher identifier", example = "9c5c0dae-5856-455c-bb64-d767545c2465")
    val id: UUID?,
    @field:Schema(description = "Resource type", example = "voucher")
    val objectType: String = "voucher",
    @field:Schema(description = "Voucher code", example = "MULTI-USE-1000")
    val code: String?,
    @field:Schema(description = "Voucher type", example = "DISCOUNT_VOUCHER")
    val type: VoucherType?,
    @field:Schema(description = "Current status", example = "ACTIVE")
    val status: String? = "ACTIVE",
    @field:Schema(description = "Discount payload if applicable")
    val discount: DiscountDto? = null,
    @field:Schema(description = "Gift payload if applicable")
    val gift: GiftDto? = null,
    @field:Schema(description = "Loyalty payload if applicable")
    val loyalty_card: LoyaltyCardDto? = null,
    @field:Schema(description = "Redemption limits and counters")
    val redemption: RedemptionDto? = null,
    @field:Schema(description = "Recurring validity timeframe")
    val validity_timeframe: ValidityTimeframeDto? = null,
    @field:Schema(description = "Days of week (0=Sun..6=Sat) when voucher is valid")
    val validity_day_of_week: List<Int>? = null,
    @field:Schema(description = "Daily validity hours")
    val validity_hours: ValidityHoursDto? = null,
    @field:Schema(description = "Additional voucher info/description", example = "Black Friday special")
    val additional_info: String? = null,
    @field:Schema(description = "UTC start date of validity", example = "2025-01-01T00:00:00Z")
    val start_date: Instant? = null,
    @field:Schema(description = "UTC expiration date", example = "2025-12-31T23:59:59Z")
    val expiration_date: Instant? = null,
    @field:Schema(description = "Arbitrary metadata", example = """{"source":"dashboard"}""")
    val metadata: Map<String, Any?>? = null,
    @field:Schema(description = "Voucher assets such as QR/barcode")
    val assets: VoucherAssetsDto? = null,
    @field:Schema(description = "Assigned categories")
    val categories: List<CategoryResponse>? = null,
    @field:Schema(description = "Campaign identifier")
    val campaign_id: UUID? = null,
    @field:Schema(description = "Creation timestamp", example = "2025-01-01T12:00:00Z")
    val created_at: Instant? = null,
    @field:Schema(description = "Update timestamp", example = "2025-01-02T12:00:00Z")
    val updated_at: Instant? = null
)
