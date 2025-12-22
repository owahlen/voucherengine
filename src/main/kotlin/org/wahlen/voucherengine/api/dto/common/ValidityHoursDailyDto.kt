package org.wahlen.voucherengine.api.dto.common

import io.swagger.v3.oas.annotations.media.Schema

data class ValidityHoursDailyDto(
    @field:Schema(description = "Start time HH:mm", example = "12:00")
    var start_time: String? = null,
    @field:Schema(description = "End time HH:mm", example = "14:00")
    var expiration_time: String? = null,
    @field:Schema(description = "Days of week (0=Sunday ... 6=Saturday)", example = "[1,2,3,4,5]")
    var days_of_week: List<Int>? = null
)
