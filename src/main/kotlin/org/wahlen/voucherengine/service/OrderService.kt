package org.wahlen.voucherengine.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.api.dto.request.OrderCreateRequest
import org.wahlen.voucherengine.api.dto.response.OrderResponse
import org.wahlen.voucherengine.persistence.model.order.Order
import org.wahlen.voucherengine.persistence.model.order.OrderItem
import org.wahlen.voucherengine.persistence.repository.OrderRepository
import java.util.UUID

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val customerService: CustomerService,
    private val tenantService: TenantService,
    private val productRepository: org.wahlen.voucherengine.persistence.repository.ProductRepository,
    private val skuRepository: org.wahlen.voucherengine.persistence.repository.SkuRepository,
) {

    @Transactional
    fun create(tenantName: String, request: OrderCreateRequest): OrderResponse {
        val tenant = tenantService.requireTenant(tenantName)
        val entity = (request.source_id?.let { orderRepository.findBySourceIdAndTenantName(it, tenantName) }) ?: Order()
        applyRequest(tenantName, entity, request)
        entity.tenant = tenant
        return toResponse(orderRepository.save(entity))
    }

    @Transactional
    fun update(tenantName: String, orderId: String, request: OrderCreateRequest): OrderResponse? {
        val entity = find(tenantName, orderId) ?: return null
        applyRequest(tenantName, entity, request.copy(source_id = orderId))
        return toResponse(orderRepository.save(entity))
    }

    @Transactional(readOnly = true)
    fun list(tenantName: String, pageable: Pageable): Page<OrderResponse> =
        orderRepository.findAllByTenantName(tenantName, pageable).map(::toResponse)

    @Transactional(readOnly = true)
    fun get(tenantName: String, id: UUID): OrderResponse? =
        orderRepository.findByIdAndTenantName(id, tenantName)?.let(::toResponse)

    @Transactional(readOnly = true)
    fun getBySource(tenantName: String, sourceId: String): OrderResponse? =
        orderRepository.findBySourceIdAndTenantName(sourceId, tenantName)?.let(::toResponse)

    @Transactional(readOnly = true)
    fun getByIdOrSource(tenantName: String, idOrSource: String): OrderResponse? =
        find(tenantName, idOrSource)?.let(::toResponse)

    @Transactional
    fun delete(tenantName: String, sourceId: String): Boolean {
        val existing = find(tenantName, sourceId) ?: return false
        orderRepository.delete(existing)
        return true
    }

    private fun find(tenantName: String, idOrSource: String): Order? {
        val uuid = runCatching { UUID.fromString(idOrSource) }.getOrNull()
        if (uuid != null) {
            val byUuid = orderRepository.findByIdAndTenantName(uuid, tenantName)
            if (byUuid != null) {
                return byUuid
            }
        }
        return orderRepository.findBySourceIdAndTenantName(idOrSource, tenantName)
    }

    private fun applyRequest(tenantName: String, entity: Order, request: OrderCreateRequest) {
        entity.sourceId = request.source_id ?: entity.sourceId
        entity.status = request.status ?: entity.status
        entity.amount = request.amount ?: entity.amount
        entity.initialAmount = request.initial_amount ?: entity.initialAmount
        entity.discountAmount = request.discount_amount ?: entity.discountAmount
        entity.metadata = request.metadata ?: entity.metadata
        entity.customer = customerService.ensureCustomer(tenantName, request.customer)
        request.items?.let { items ->
            items.forEach { item ->
                if (item.product_id.isNullOrBlank() && item.sku_id.isNullOrBlank() && item.product?.id.isNullOrBlank() && item.sku?.id.isNullOrBlank()) {
                    throw org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST,
                        "order.items must include product_id or sku_id"
                    )
                }
            }
            entity.items.clear()
            val tenant = tenantService.requireTenant(tenantName)
            items.forEach { item ->
                val productId = item.product_id ?: item.product?.id
                val skuId = item.sku_id ?: item.sku?.id
                val amount = item.amount ?: calculateAmount(item.price, item.quantity)
                val discountAmount = item.discount_amount ?: 0L
                val subtotalAmount = item.subtotal_amount ?: (amount - discountAmount)
                val productSnapshot = resolveProductSnapshot(tenantName, productId, item.product)
                val skuSnapshot = resolveSkuSnapshot(tenantName, skuId, item.sku)
                val enrichedProductSnapshot = productSnapshot ?: skuId?.let { idOrSource ->
                    findSkuEntity(tenantName, idOrSource)?.product?.let(::productSnapshotFromEntity)
                }
                val relatedObject = item.related_object ?: when {
                    !skuId.isNullOrBlank() -> "sku"
                    !productId.isNullOrBlank() -> "product"
                    else -> null
                }
                val orderItem = OrderItem(
                    productId = productId,
                    skuId = skuId,
                    sourceId = item.source_id,
                    relatedObject = relatedObject,
                    quantity = item.quantity,
                    amount = amount,
                    discountAmount = discountAmount,
                    subtotalAmount = subtotalAmount,
                    price = item.price,
                    productSnapshot = enrichedProductSnapshot,
                    skuSnapshot = skuSnapshot,
                    metadata = item.metadata,
                    order = entity
                )
                orderItem.tenant = tenant
                entity.items.add(orderItem)
            }
        }
    }

    private fun toResponse(order: Order): OrderResponse =
        OrderResponse(
            id = order.id,
            source_id = order.sourceId,
            status = order.status,
            amount = order.amount,
            initial_amount = order.initialAmount,
            discount_amount = order.discountAmount,
            items_discount_amount = order.items.sumOf { it.discountAmount ?: 0L },
            total_discount_amount = (order.discountAmount ?: 0L) + order.items.sumOf { it.discountAmount ?: 0L },
            total_amount = (order.amount ?: order.items.sumOf { it.amount ?: 0L }) -
                (order.discountAmount ?: 0L) -
                order.items.sumOf { it.discountAmount ?: 0L },
            metadata = order.metadata,
            items = order.items.map {
                org.wahlen.voucherengine.api.dto.response.OrderItemResponse(
                    product_id = it.productId,
                    sku_id = it.skuId,
                    source_id = it.sourceId,
                    related_object = it.relatedObject,
                    quantity = it.quantity,
                    amount = it.amount,
                    discount_amount = it.discountAmount,
                    subtotal_amount = it.subtotalAmount,
                    price = it.price,
                    product = it.productSnapshot?.let { snapshot ->
                        org.wahlen.voucherengine.api.dto.response.OrderItemProductResponse(
                            id = snapshot["id"] as? String,
                            source_id = snapshot["source_id"] as? String,
                            name = snapshot["name"] as? String,
                            price = (snapshot["price"] as? Number)?.toLong(),
                            metadata = snapshot["metadata"] as? Map<String, Any?>
                        )
                    },
                    sku = it.skuSnapshot?.let { snapshot ->
                        org.wahlen.voucherengine.api.dto.response.OrderItemSkuResponse(
                            id = snapshot["id"] as? String,
                            source_id = snapshot["source_id"] as? String,
                            sku = snapshot["sku"] as? String,
                            price = (snapshot["price"] as? Number)?.toLong(),
                            metadata = snapshot["metadata"] as? Map<String, Any?>
                        )
                    },
                    metadata = it.metadata
                )
            },
            customer_id = order.customer?.id,
            customer = order.customer?.id?.let { org.wahlen.voucherengine.api.dto.response.OrderCustomerResponse(id = it) },
            created_at = order.createdAt,
            updated_at = order.updatedAt
        )

    private fun calculateAmount(price: Long?, quantity: Int?): Long =
        (price ?: 0L) * (quantity ?: 0)

    private fun resolveProductSnapshot(
        tenantName: String,
        productIdOrSource: String?,
        request: org.wahlen.voucherengine.api.dto.request.OrderItemProductRequest?
    ): Map<String, Any?>? {
        if (request != null) {
            return mapOf(
                "id" to request.id,
                "source_id" to request.source_id,
                "name" to request.name,
                "price" to request.price,
                "metadata" to request.metadata
            )
        }
        if (productIdOrSource.isNullOrBlank()) return null
        val uuid = runCatching { java.util.UUID.fromString(productIdOrSource) }.getOrNull()
        val product = if (uuid != null) {
            productRepository.findByIdAndTenantName(uuid, tenantName)
        } else {
            productRepository.findBySourceIdAndTenantName(productIdOrSource, tenantName)
        } ?: return null
        return mapOf(
            "id" to product.id?.toString(),
            "source_id" to product.sourceId,
            "name" to product.name,
            "price" to product.price,
            "metadata" to product.metadata
        )
    }

    private fun resolveSkuSnapshot(
        tenantName: String,
        skuIdOrSource: String?,
        request: org.wahlen.voucherengine.api.dto.request.OrderItemSkuRequest?
    ): Map<String, Any?>? {
        if (request != null) {
            return mapOf(
                "id" to request.id,
                "source_id" to request.source_id,
                "sku" to request.sku,
                "price" to request.price,
                "metadata" to request.metadata
            )
        }
        if (skuIdOrSource.isNullOrBlank()) return null
        val sku = findSkuEntity(tenantName, skuIdOrSource) ?: return null
        return mapOf(
            "id" to sku.id?.toString(),
            "source_id" to sku.sourceId,
            "sku" to sku.sku,
            "price" to sku.price,
            "metadata" to sku.metadata
        )
    }

    private fun findSkuEntity(tenantName: String, idOrSource: String): org.wahlen.voucherengine.persistence.model.product.Sku? {
        val uuid = runCatching { java.util.UUID.fromString(idOrSource) }.getOrNull()
        return if (uuid != null) {
            skuRepository.findByIdAndTenantName(uuid, tenantName)
        } else {
            skuRepository.findBySourceIdAndTenantName(idOrSource, tenantName)
        }
    }

    private fun productSnapshotFromEntity(product: org.wahlen.voucherengine.persistence.model.product.Product): Map<String, Any?> =
        mapOf(
            "id" to product.id?.toString(),
            "source_id" to product.sourceId,
            "name" to product.name,
            "price" to product.price,
            "metadata" to product.metadata
        )
}
