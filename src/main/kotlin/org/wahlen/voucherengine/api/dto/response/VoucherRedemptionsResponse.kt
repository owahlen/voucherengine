package org.wahlen.voucherengine.api.dto.response

import java.util.UUID

data class VoucherRedemptionsResponse(
    val quantity: Int? = null,
    val redeemed_quantity: Int? = null,
    val `object`: String = "list",
    val url: String? = null,
    val data_ref: String = "redemption_entries",
    val total: Int = 0,
    val redemption_entries: List<RedemptionEntryResponse> = emptyList()
)

data class RedemptionEntryResponse(
    val id: UUID? = null,
    val `object`: String = "redemption",
    val date: java.time.Instant? = null,
    val customer_id: UUID? = null,
    val tracking_id: String? = null,
    val metadata: Map<String, Any?>? = null,
    val result: String? = null,
    val status: String? = null,
    val amount: Long? = null,
    val voucher: VoucherReferenceDto? = null
)

data class VoucherReferenceDto(
    val id: UUID? = null,
    val code: String? = null,
    val campaign_id: UUID? = null
)
