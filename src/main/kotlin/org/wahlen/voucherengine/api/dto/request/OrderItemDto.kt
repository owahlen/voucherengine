package org.wahlen.voucherengine.api.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Positive

data class OrderItemDto(
    @field:Schema(description = "Product identifier", example = "prod_123")
    var product_id: String? = null,
    @field:Schema(description = "SKU identifier", example = "sku_456")
    var sku_id: String? = null,
    @field:Schema(description = "Item source id", example = "item-1")
    var source_id: String? = null,
    @field:Schema(description = "Related object type", example = "product")
    var related_object: String? = null,
    @field:Positive
    @field:Schema(description = "Quantity of the product", example = "2")
    var quantity: Int? = null,
    @field:Schema(description = "Item amount", example = "10000")
    var amount: Long? = null,
    @field:Schema(description = "Item discount amount", example = "500")
    var discount_amount: Long? = null,
    @field:Schema(description = "Item subtotal amount", example = "9500")
    var subtotal_amount: Long? = null,
    @field:Positive
    @field:Schema(description = "Price per item in minor currency units", example = "500")
    var price: Long? = null,
    @field:Schema(description = "Product snapshot")
    var product: OrderItemProductRequest? = null,
    @field:Schema(description = "SKU snapshot")
    var sku: OrderItemSkuRequest? = null,
    @field:Schema(description = "Item metadata")
    var metadata: Map<String, Any?>? = null
)
