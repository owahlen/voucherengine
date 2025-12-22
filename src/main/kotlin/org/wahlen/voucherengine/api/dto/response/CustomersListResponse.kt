package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.wahlen.voucherengine.persistence.model.customer.Customer

data class CustomersListResponse(
    @field:Schema(description = "Object marker", example = "list")
    val `object`: String = "list",
    @field:Schema(description = "Data reference", example = "customers")
    val data_ref: String = "customers",
    @field:Schema(description = "Customers list")
    val customers: List<Customer> = emptyList(),
    @field:Schema(description = "Total number of customers")
    val total: Int = 0
)
