package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class QualificationGiftResult(
    @field:Schema(description = "Gift card balance")
    val balance: Long? = null,
    @field:Schema(description = "Gift card credits")
    val credits: Long? = null,
    @field:Schema(description = "Locked credits under validation session")
    val locked_credits: Long? = null
)
