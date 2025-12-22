package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.wahlen.voucherengine.api.dto.request.ExportParameters
import java.time.Instant
import java.util.UUID

data class ExportResponse(
    @field:Schema(description = "Export id")
    val id: UUID? = null,
    @field:Schema(description = "Object marker", example = "export")
    val `object`: String = "export",
    @field:Schema(description = "Created timestamp")
    val created_at: Instant? = null,
    @field:Schema(description = "Export status", example = "DONE")
    val status: String? = null,
    @field:Schema(description = "Export channel", example = "API")
    val channel: String? = null,
    @field:Schema(description = "Export result")
    val result: ExportResultResponse? = null,
    @field:Schema(description = "User id")
    val user_id: String? = null,
    @field:Schema(description = "Exported object", example = "voucher")
    val exported_object: String? = null,
    @field:Schema(description = "Export parameters")
    val parameters: ExportParameters? = null
)
