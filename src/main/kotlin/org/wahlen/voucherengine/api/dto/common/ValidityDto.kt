package org.wahlen.voucherengine.api.dto.common

import io.swagger.v3.oas.annotations.media.Schema

data class ValidityTimeframeDto(
    @field:Schema(description = "ISO 8601 duration the voucher stays active in each interval", example = "PT1H")
    var duration: String? = null,
    @field:Schema(description = "ISO 8601 interval between activations", example = "P2D")
    var interval: String? = null
)

data class ValidityHoursDailyDto(
    @field:Schema(description = "Start time HH:mm", example = "12:00")
    var start_time: String? = null,
    @field:Schema(description = "End time HH:mm", example = "14:00")
    var expiration_time: String? = null,
    @field:Schema(description = "Days of week (0=Sunday ... 6=Saturday)", example = "[1,2,3,4,5]")
    var days_of_week: List<Int>? = null
)

data class ValidityHoursDto(
    @field:Schema(description = "Daily time windows", example = """{"daily":[{"start_time":"12:00","expiration_time":"14:00","days_of_week":[1,2,3,4,5]}]}""")
    var daily: List<ValidityHoursDailyDto>? = null
)
