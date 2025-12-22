package org.wahlen.voucherengine.api.dto.common

import io.swagger.v3.oas.annotations.media.Schema

data class SessionDto(
    @field:Schema(description = "Session key", example = "sess_123")
    val key: String? = null,
    @field:Schema(description = "Session type", example = "LOCK")
    val type: String? = null,
    @field:Schema(description = "Time to live value")
    val ttl: Long? = null,
    @field:Schema(description = "Time to live unit", example = "DAYS")
    val ttl_unit: String? = null
)
