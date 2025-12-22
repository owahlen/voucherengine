package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class QualificationItemList(
    @field:Schema(description = "Object marker", example = "list")
    val `object`: String = "list",
    @field:Schema(description = "Data reference", example = "data")
    val data_ref: String = "data",
    @field:Schema(description = "Items")
    val data: List<QualificationItemRef> = emptyList(),
    @field:Schema(description = "Total items")
    val total: Int = 0
)
