package org.wahlen.voucherengine.api.dto.response

import java.time.Instant

data class SegmentResponse(
    val id: String,
    val name: String,
    val created_at: Instant?,
    val type: String,
    val filter: Map<String, Any?>? = null,
    val `object`: String = "segment"
)

data class SegmentListResponse(
    val `object`: String = "list",
    val data_ref: String = "segments",
    val segments: List<SegmentResponse>
)

data class CustomerSegmentResponse(
    val id: String,
    val name: String,
    val `object`: String = "segment"
)

data class CustomerSegmentsListResponse(
    val `object`: String = "list",
    val data_ref: String = "data",
    val data: List<CustomerSegmentResponse>
)
