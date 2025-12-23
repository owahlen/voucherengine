package org.wahlen.voucherengine.api.dto.response

import java.time.Instant
import java.util.UUID

data class VoucherTransactionResponse(
    val id: UUID? = null,
    val source_id: String? = null,
    val voucher_id: UUID? = null,
    val campaign_id: UUID? = null,
    val type: String? = null,
    val source: String? = null,
    val reason: String? = null,
    val amount: Long? = null,
    val created_at: Instant? = null,
    val details: VoucherTransactionDetailsDto? = null
)

data class VoucherTransactionDetailsDto(
    val balance: VoucherBalanceDto? = null,
    val order: OrderReferenceDto? = null,
    val redemption: RedemptionReferenceDto? = null,
    val rollback: RollbackReferenceDto? = null
)

data class VoucherBalanceDto(
    val type: String? = null,
    val total: Long? = null,
    val balance: Long? = null,
    val `object`: String = "balance"
)

data class OrderReferenceDto(
    val id: UUID? = null,
    val source_id: String? = null
)

data class RedemptionReferenceDto(
    val id: UUID? = null
)

data class RollbackReferenceDto(
    val id: UUID? = null
)

data class VoucherTransactionsListResponse(
    val `object`: String = "list",
    val data_ref: String = "data",
    val data: List<VoucherTransactionResponse> = emptyList(),
    val has_more: Boolean = false,
    val total: Int = 0
)
