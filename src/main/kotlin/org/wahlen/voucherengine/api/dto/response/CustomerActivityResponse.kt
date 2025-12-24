package org.wahlen.voucherengine.api.dto.response

import java.time.Instant

/**
 * Response for customer activity list endpoint.
 */
data class CustomerActivityResponse(
    val `object`: String = "list",
    val data_ref: String = "data",
    val data: List<CustomerActivityDto>,
    val has_more: Boolean,
    val more_starting_after: String? = null
)

/**
 * Individual customer activity event.
 */
data class CustomerActivityDto(
    val id: String,
    val type: String,
    val data: Map<String, Any?>,
    val created_at: Instant?,
    val group_id: String? = null
)
