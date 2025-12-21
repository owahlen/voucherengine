package org.wahlen.voucherengine.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.wahlen.voucherengine.persistence.model.customer.Customer
import java.util.UUID

interface CustomerRepository : JpaRepository<Customer, UUID>
