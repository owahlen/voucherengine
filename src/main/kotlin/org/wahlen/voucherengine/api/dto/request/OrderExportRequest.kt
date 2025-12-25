package org.wahlen.voucherengine.api.dto.request

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Request for exporting orders.
 * Based on Voucherify OrdersExportCreateRequestBody schema.
 */
@Schema(description = "Order export request")
data class OrderExportRequest(
    @Schema(description = "Export parameters including fields, filters, and order")
    val parameters: OrderExportParameters? = null
)

