package org.wahlen.voucherengine.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.wahlen.voucherengine.persistence.model.order.Order
import java.util.UUID

interface OrderRepository : JpaRepository<Order, UUID> {
    fun findBySourceId(sourceId: String): Order?
}
