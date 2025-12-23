package org.wahlen.voucherengine.api.dto.response

import java.time.Instant
import java.util.UUID

/**
 * Response for async job status queries
 */
data class AsyncJobStatusResponse(
    val id: UUID,
    val type: String,
    val status: String,
    val progress: Int,
    val total: Int,
    val result: Map<String, Any?>? = null,
    val error_message: String? = null,
    val created_at: Instant? = null,
    val started_at: Instant? = null,
    val completed_at: Instant? = null
)
