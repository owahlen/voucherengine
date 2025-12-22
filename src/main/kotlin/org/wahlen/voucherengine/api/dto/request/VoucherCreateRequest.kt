package org.wahlen.voucherengine.api.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.wahlen.voucherengine.api.dto.common.DiscountDto
import org.wahlen.voucherengine.api.dto.common.GiftDto
import org.wahlen.voucherengine.api.dto.common.LoyaltyCardDto
import org.wahlen.voucherengine.api.dto.common.RedemptionDto

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
    @field:Valid
    @field:Schema(description = "Holder of the voucher if applicable")
    var customer: CustomerReferenceDto? = null,
    @field:Schema(description = "Arbitrary metadata", example = """{"source":"dashboard"}""")
    var metadata: Map<String, Any?>? = null,
    @field:Schema(description = "Explicit active flag", example = "true")
    var active: Boolean? = null,
)
