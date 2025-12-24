package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.wahlen.voucherengine.api.dto.common.DiscountDto
import org.wahlen.voucherengine.api.dto.common.GiftDto
import org.wahlen.voucherengine.api.dto.common.LoyaltyCardDto

data class PublicationVoucherDto(
    @field:Schema(description = "Voucher id")
    val id: java.util.UUID? = null,
    @field:Schema(description = "Voucher code")
    val code: String? = null,
    @field:Schema(description = "Object marker", example = "voucher")
    val `object`: String = "voucher",
    @field:Schema(description = "Campaign name")
    val campaign: String? = null,
    @field:Schema(description = "Campaign id")
    val campaign_id: java.util.UUID? = null,
    @field:Schema(description = "Voucher type")
    val type: String? = null,
    @field:Schema(description = "Discount payload")
    val discount: DiscountDto? = null,
    @field:Schema(description = "Gift payload")
    val gift: GiftDto? = null,
    @field:Schema(description = "Loyalty card payload")
    val loyalty_card: LoyaltyCardDto? = null,
    @field:Schema(description = "Start date")
    val start_date: java.time.Instant? = null,
    @field:Schema(description = "Expiration date")
    val expiration_date: java.time.Instant? = null,
    @field:Schema(description = "Validity timeframe")
    val validity_timeframe: org.wahlen.voucherengine.api.dto.common.ValidityTimeframeDto? = null,
    @field:Schema(description = "Validity days of week")
    val validity_day_of_week: List<Int>? = null,
    @field:Schema(description = "Validity hours")
    val validity_hours: org.wahlen.voucherengine.api.dto.common.ValidityHoursDto? = null,
    @field:Schema(description = "Active flag")
    val active: Boolean? = null,
    @field:Schema(description = "Additional info")
    val additional_info: String? = null,
    @field:Schema(description = "Metadata")
    val metadata: Map<String, Any?>? = null,
    @field:Schema(description = "Assets")
    val assets: VoucherAssetsDto? = null,
    @field:Schema(description = "Whether this voucher is a referral code")
    val is_referral_code: Boolean? = null
)
