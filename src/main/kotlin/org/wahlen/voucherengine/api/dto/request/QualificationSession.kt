package org.wahlen.voucherengine.api.dto.request

import io.swagger.v3.oas.annotations.media.Schema

data class QualificationSession(
    @field:Schema(description = "Session type", example = "LOCK")
    var type: String? = null
)
