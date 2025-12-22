package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

data class PublicationResponse(
    @field:Schema(description = "Publication id")
    val id: UUID?,
    @field:Schema(description = "Object marker", example = "publication")
    val `object`: String = "publication",
    @field:Schema(description = "Created timestamp")
    val created_at: Instant?,
    @field:Schema(description = "Customer id")
    val customer_id: UUID?,
    @field:Schema(description = "Customer source id")
    val tracking_id: String? = null,
    @field:Schema(description = "Publication metadata")
    val metadata: Map<String, Any?>? = null,
    @field:Schema(description = "Publication channel")
    val channel: String? = null,
    @field:Schema(description = "Publication source id")
    val source_id: String? = null,
    @field:Schema(description = "Publication result", example = "SUCCESS")
    val result: String? = null,
    @field:Schema(description = "Failure code")
    val failure_code: String? = null,
    @field:Schema(description = "Failure message")
    val failure_message: String? = null,
    @field:Schema(description = "Customer object")
    val customer: PublicationCustomerDto? = null,
    @field:Schema(description = "Voucher object")
    val voucher: PublicationVoucherDto? = null,
    @field:Schema(description = "Voucher codes assigned")
    val vouchers: List<String>? = null,
    @field:Schema(description = "Voucher ids assigned")
    val vouchers_id: List<UUID>? = null
)
