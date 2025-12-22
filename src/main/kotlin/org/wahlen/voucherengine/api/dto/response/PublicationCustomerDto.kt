package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

data class PublicationCustomerDto(
    @field:Schema(description = "Customer id")
    val id: UUID?,
    @field:Schema(description = "Customer name")
    val name: String? = null,
    @field:Schema(description = "Customer email")
    val email: String? = null,
    @field:Schema(description = "Customer source id")
    val source_id: String? = null,
    @field:Schema(description = "Customer metadata")
    val metadata: Map<String, Any?>? = null,
    @field:Schema(description = "Object marker", example = "customer")
    val `object`: String = "customer"
)
