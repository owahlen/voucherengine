package org.wahlen.voucherengine.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.wahlen.voucherengine.persistence.model.order.Order
import java.util.*

interface OrderRepository : JpaRepository<Order, UUID>
