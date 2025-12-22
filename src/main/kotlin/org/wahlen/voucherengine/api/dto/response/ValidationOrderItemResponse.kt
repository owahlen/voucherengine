package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class ValidationOrderItemResponse(
    @field:Schema(description = "Product identifier")
    val product_id: String? = null,
    @field:Schema(description = "SKU identifier")
    val sku_id: String? = null,
    @field:Schema(description = "Item source id")
    val source_id: String? = null,
    @field:Schema(description = "Related object type")
    val related_object: String? = null,
    @field:Schema(description = "Quantity")
    val quantity: Int? = null,
    @field:Schema(description = "Item amount")
    val amount: Long? = null,
    @field:Schema(description = "Item discount amount")
    val discount_amount: Long? = null,
    @field:Schema(description = "Item subtotal amount")
    val subtotal_amount: Long? = null,
    @field:Schema(description = "Unit price in minor units")
    val price: Long? = null,
    @field:Schema(description = "Item metadata")
    val metadata: Map<String, Any?>? = null,
    @field:Schema(description = "Application details for replacements")
    val application_details: List<Map<String, Any?>>? = null,
    @field:Schema(description = "Object type", example = "order_item")
    val `object`: String = "order_item"
)
