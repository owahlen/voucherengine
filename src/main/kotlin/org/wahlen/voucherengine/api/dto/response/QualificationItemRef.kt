package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class QualificationItemRef(
    @field:Schema(description = "Object type", example = "product")
    val `object`: String? = null,
    @field:Schema(description = "Object id")
    val id: String? = null
)
