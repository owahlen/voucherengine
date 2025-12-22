package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class OrderItemResponse(
    @field:Schema(description = "Product identifier", example = "prod_123")
    val product_id: String? = null,
    @field:Schema(description = "SKU identifier", example = "sku_456")
    val sku_id: String? = null,
    @field:Schema(description = "Item source id", example = "item-1")
    val source_id: String? = null,
    @field:Schema(description = "Related object type", example = "product")
    val related_object: String? = null,
    @field:Schema(description = "Quantity", example = "2")
    val quantity: Int? = null,
    @field:Schema(description = "Item amount", example = "10000")
    val amount: Long? = null,
    @field:Schema(description = "Item discount amount", example = "500")
    val discount_amount: Long? = null,
    @field:Schema(description = "Item subtotal amount", example = "9500")
    val subtotal_amount: Long? = null,
    @field:Schema(description = "Unit price in minor units", example = "10000")
    val price: Long? = null,
    @field:Schema(description = "Product snapshot")
    val product: OrderItemProductResponse? = null,
    @field:Schema(description = "SKU snapshot")
    val sku: OrderItemSkuResponse? = null,
    @field:Schema(description = "Item metadata")
    val metadata: Map<String, Any?>? = null,
    @field:Schema(description = "Object type", example = "order_item")
    val `object`: String = "order_item"
)
