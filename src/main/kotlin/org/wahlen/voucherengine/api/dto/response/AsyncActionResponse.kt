package org.wahlen.voucherengine.api.dto.response

data class AsyncActionResponse(
    val async_action_id: String,
    val status: String = "ACCEPTED",
    val `object`: String = "async_action"
)

data class BulkOperationResponse(
    val success_count: Int = 0,
    val failure_count: Int = 0,
    val failed_codes: List<String> = emptyList(),
    val `object`: String = "bulk_result"
)
