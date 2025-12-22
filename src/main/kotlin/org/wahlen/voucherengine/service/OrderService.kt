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
    private val tenantService: TenantService,
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
    fun list(tenantName: String): List<OrderResponse> = orderRepository.findAllByTenantName(tenantName).map(::toResponse)

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
