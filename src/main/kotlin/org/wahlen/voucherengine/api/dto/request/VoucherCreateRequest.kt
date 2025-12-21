package org.wahlen.voucherengine.api.dto.request

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.wahlen.voucherengine.api.dto.DiscountDto
import org.wahlen.voucherengine.api.dto.GiftDto
import org.wahlen.voucherengine.api.dto.LoyaltyCardDto
import org.wahlen.voucherengine.api.dto.RedemptionDto

data class VoucherCreateRequest(
    @field:NotBlank
    val code: String? = null,
    @field:NotBlank
    val type: String? = null,
    @field:Valid
    val discount: DiscountDto? = null,
    @field:Valid
    val gift: GiftDto? = null,
    @field:Valid
    val loyalty_card: LoyaltyCardDto? = null,
    @field:Valid
    val redemption: RedemptionDto? = null,
    @field:Valid
    val customer: CustomerReferenceDto? = null,
    val metadata: Map<String, Any?>? = null,
    val active: Boolean? = null,
)
