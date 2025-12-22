package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class VouchersListResponse(
    @field:Schema(description = "Object marker", example = "list")
    val `object`: String = "list",
    @field:Schema(description = "Data reference", example = "vouchers")
    val data_ref: String = "vouchers",
    @field:Schema(description = "Vouchers list")
    val vouchers: List<VoucherResponse> = emptyList(),
    @field:Schema(description = "Total number of vouchers")
    val total: Int = 0
)
