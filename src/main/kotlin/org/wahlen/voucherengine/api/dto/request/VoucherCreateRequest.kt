package org.wahlen.voucherengine.api.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.wahlen.voucherengine.api.dto.common.DiscountDto
import org.wahlen.voucherengine.api.dto.common.GiftDto
import org.wahlen.voucherengine.api.dto.common.LoyaltyCardDto
import org.wahlen.voucherengine.api.dto.common.RedemptionDto
import org.wahlen.voucherengine.api.dto.common.ValidityHoursDto
import org.wahlen.voucherengine.api.dto.common.ValidityTimeframeDto

data class VoucherCreateRequest(
    @field:NotBlank
    @field:Schema(description = "Voucher code", example = "SUMMER-10")
    var code: String? = null,
    @field:NotBlank
    @field:Schema(description = "Voucher type (DISCOUNT_VOUCHER, GIFT_VOUCHER, LOYALTY_CARD)", example = "DISCOUNT_VOUCHER")
    var type: String? = null,
    @field:Valid
    @field:Schema(description = "Discount payload for discount vouchers")
    var discount: DiscountDto? = null,
    @field:Valid
    @field:Schema(description = "Gift payload for gift vouchers")
    var gift: GiftDto? = null,
    @field:Valid
    @field:Schema(description = "Loyalty payload for loyalty cards")
    var loyalty_card: LoyaltyCardDto? = null,
    @field:Valid
    @field:Schema(description = "Redemption limits and counters")
    var redemption: RedemptionDto? = null,
    @field:Schema(description = "UTC start date of validity", example = "2025-01-01T00:00:00Z")
    var start_date: java.time.Instant? = null,
    @field:Schema(description = "UTC expiration date", example = "2025-12-31T23:59:59Z")
    var expiration_date: java.time.Instant? = null,
    @field:Schema(description = "Recurring validity timeframe", example = """{"duration":"PT1H","interval":"P2D"}""")
    var validity_timeframe: ValidityTimeframeDto? = null,
    @field:Schema(description = "Days of week (0=Sun..6=Sat) when voucher is valid", example = "[1,2,3,4,5]")
    var validity_day_of_week: List<Int>? = null,
    @field:Schema(description = "Daily validity hours", example = """{"daily":[{"start_time":"09:00","expiration_time":"12:00","days_of_week":[1,2,3,4,5]}]}""")
    var validity_hours: ValidityHoursDto? = null,
    @field:Valid
    @field:Schema(description = "Holder of the voucher if applicable")
    var customer: CustomerReferenceDto? = null,
    @field:Schema(description = "Arbitrary metadata", example = """{"source":"dashboard"}""")
    var metadata: Map<String, Any?>? = null,
    @field:Schema(description = "Explicit active flag", example = "true")
    var active: Boolean? = null,
    @field:Schema(description = "Additional voucher info/description", example = "Black Friday special")
    var additional_info: String? = null,
    @field:Schema(description = "Category IDs to assign to the voucher", example = """["11111111-1111-1111-1111-111111111111"]""")
    var category_ids: List<java.util.UUID>? = null,
    @field:Schema(description = "Campaign ID to link the voucher to", example = "22222222-2222-2222-2222-222222222222")
    var campaign_id: java.util.UUID? = null
)
