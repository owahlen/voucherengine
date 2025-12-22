package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class PublicationsListResponse(
    @field:Schema(description = "Object marker", example = "list")
    val `object`: String = "list",
    @field:Schema(description = "Data reference", example = "publications")
    val data_ref: String = "publications",
    @field:Schema(description = "Publications list")
    val publications: List<PublicationResponse> = emptyList(),
    @field:Schema(description = "Total number of publications")
    val total: Int = 0
)
