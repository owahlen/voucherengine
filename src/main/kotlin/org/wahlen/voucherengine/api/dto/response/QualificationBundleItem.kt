package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class QualificationBundleItem(
    @field:Schema(description = "Object type", example = "product")
    val `object`: String? = null,
    @field:Schema(description = "Object id")
    val id: String? = null,
    @field:Schema(description = "Item quantity")
    val item_quantity: Int? = null
)
