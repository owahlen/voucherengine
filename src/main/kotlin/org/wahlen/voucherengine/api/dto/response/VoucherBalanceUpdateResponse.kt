package org.wahlen.voucherengine.api.dto.response

import java.util.UUID

data class VoucherBalanceUpdateResponse(
    val amount: Long? = null,
    val total: Long? = null,
    val balance: Long? = null,
    val type: String? = null,
    val `object`: String = "balance",
    val related_object: RelatedObjectDto? = null
)

data class RelatedObjectDto(
    val type: String? = null,
    val id: UUID? = null
)
