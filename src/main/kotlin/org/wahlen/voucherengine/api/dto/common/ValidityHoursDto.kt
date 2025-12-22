package org.wahlen.voucherengine.api.dto.common

import io.swagger.v3.oas.annotations.media.Schema

data class ValidityHoursDto(
    @field:Schema(description = "Daily time windows", example = """{"daily":[{"start_time":"12:00","expiration_time":"14:00","days_of_week":[1,2,3,4,5]}]}""")
    var daily: List<ValidityHoursDailyDto>? = null
)
