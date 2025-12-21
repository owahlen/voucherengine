package org.wahlen.voucherengine.api.dto.request

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.wahlen.voucherengine.api.dto.DiscountDto
import org.wahlen.voucherengine.api.dto.GiftDto
import org.wahlen.voucherengine.api.dto.LoyaltyCardDto
import org.wahlen.voucherengine.api.dto.RedemptionDto

data class VoucherCreateRequest(
    @field:NotBlank
    var code: String? = null,
    @field:NotBlank
    var type: String? = null,
    @field:Valid
    var discount: DiscountDto? = null,
    @field:Valid
    var gift: GiftDto? = null,
    @field:Valid
    var loyalty_card: LoyaltyCardDto? = null,
    @field:Valid
    var redemption: RedemptionDto? = null,
    @field:Valid
    var customer: CustomerReferenceDto? = null,
    var metadata: Map<String, Any?>? = null,
    var active: Boolean? = null,
)
