package org.wahlen.voucherengine.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.api.dto.request.OrderCreateRequest
import org.wahlen.voucherengine.api.dto.response.OrderResponse
import org.wahlen.voucherengine.persistence.model.order.Order
import org.wahlen.voucherengine.persistence.repository.OrderRepository
import java.util.UUID

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val customerService: CustomerService,
) {

    @Transactional
    fun create(request: OrderCreateRequest): OrderResponse {
        val entity = (request.id?.let { orderRepository.findBySourceId(it) }) ?: Order()
        applyRequest(entity, request)
        return toResponse(orderRepository.save(entity))
    }

    @Transactional
    fun update(orderId: String, request: OrderCreateRequest): OrderResponse? {
        val entity = find(orderId) ?: return null
        applyRequest(entity, request.copy(id = orderId))
        return toResponse(orderRepository.save(entity))
    }

    @Transactional(readOnly = true)
    fun list(): List<OrderResponse> = orderRepository.findAll().map(::toResponse)

    @Transactional(readOnly = true)
    fun get(id: UUID): OrderResponse? = orderRepository.findById(id).orElse(null)?.let(::toResponse)

    @Transactional(readOnly = true)
    fun getBySource(sourceId: String): OrderResponse? = orderRepository.findBySourceId(sourceId)?.let(::toResponse)

    @Transactional(readOnly = true)
    fun getByIdOrSource(idOrSource: String): OrderResponse? = find(idOrSource)?.let(::toResponse)

    @Transactional
    fun delete(sourceId: String): Boolean {
        val existing = find(sourceId) ?: return false
        orderRepository.delete(existing)
        return true
    }

    private fun find(idOrSource: String): Order? {
        val uuid = runCatching { UUID.fromString(idOrSource) }.getOrNull()
        if (uuid != null) {
            val byUuid = orderRepository.findById(uuid).orElse(null)
            if (byUuid != null) {
                return byUuid
            }
        }
        return orderRepository.findBySourceId(idOrSource)
    }

    private fun applyRequest(entity: Order, request: OrderCreateRequest) {
        entity.sourceId = request.id ?: entity.sourceId
        entity.status = request.status ?: entity.status
        entity.amount = request.amount ?: entity.amount
        entity.initialAmount = request.initial_amount ?: entity.initialAmount
        entity.discountAmount = request.discount_amount ?: entity.discountAmount
        entity.metadata = request.metadata ?: entity.metadata
        entity.customer = customerService.ensureCustomer(request.customer)
    }

    private fun toResponse(order: Order): OrderResponse =
        OrderResponse(
            id = order.id,
            source_id = order.sourceId,
            status = order.status,
            amount = order.amount,
            initial_amount = order.initialAmount,
            discount_amount = order.discountAmount,
            metadata = order.metadata,
            customer_id = order.customer?.id,
            created_at = order.createdAt,
            updated_at = order.updatedAt
        )
}
