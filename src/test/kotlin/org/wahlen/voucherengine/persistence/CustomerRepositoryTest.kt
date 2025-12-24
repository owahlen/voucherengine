package org.wahlen.voucherengine.persistence

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.config.IntegrationTest
import org.wahlen.voucherengine.persistence.model.customer.Customer
import org.wahlen.voucherengine.persistence.model.tenant.Tenant
import org.wahlen.voucherengine.persistence.repository.CustomerRepository
import org.wahlen.voucherengine.persistence.repository.TenantRepository
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@IntegrationTest
@Transactional
class CustomerRepositoryTest @Autowired constructor(
    private val customerRepository: CustomerRepository,
    private val tenantRepository: TenantRepository
) {
    private val tenantName = "test-tenant"
    private lateinit var tenant: Tenant

    @BeforeEach
    fun setUp() {
        tenant = tenantRepository.findByName(tenantName) ?: tenantRepository.save(Tenant(name = tenantName))
    }

    @Test
    fun `customer CRUD persists metadata`() {
        val customer = Customer(
            sourceId = "customer-123",
            name = "Alice Example",
            metadata = mapOf("tier" to "gold"),
            tenant = tenant
        )

        val saved = customerRepository.save(customer)
        val fetched = customerRepository.findByIdOrNull(saved.id!!)

        assertNotNull(fetched)
        assertEquals("Alice Example", fetched.name)
        assertEquals("gold", fetched.metadata?.get("tier"))

        fetched.name = "Alice Updated"
        val updated = customerRepository.save(fetched)

        assertEquals("Alice Updated", updated.name)

        customerRepository.delete(updated)
        assertNull(customerRepository.findByIdOrNull(updated.id!!))
    }
}
