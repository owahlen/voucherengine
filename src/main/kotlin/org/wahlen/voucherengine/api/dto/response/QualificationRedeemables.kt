package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

data class QualificationRedeemables(
    @field:Schema(description = "Object marker", example = "list")
    val `object`: String = "list",
    @field:Schema(description = "Data reference", example = "data")
    val data_ref: String = "data",
    @field:Schema(description = "Redeemables list")
    val data: List<QualificationRedeemable> = emptyList(),
    @field:Schema(description = "Total number of redeemables")
    val total: Int = 0,
    @field:Schema(description = "More records available")
    val has_more: Boolean = false,
    @field:Schema(description = "Cursor for next page")
    val more_starting_after: Instant? = null
)
