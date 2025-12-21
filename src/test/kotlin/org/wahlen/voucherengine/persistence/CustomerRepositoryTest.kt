package org.wahlen.voucherengine.persistence

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.persistence.model.customer.Customer
import org.wahlen.voucherengine.persistence.repository.CustomerRepository
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CustomerRepositoryTest @Autowired constructor(
    private val customerRepository: CustomerRepository
) {

    @Test
    fun `customer CRUD persists metadata`() {
        val customer = Customer(
            sourceId = "customer-123",
            name = "Alice Example",
            metadata = mapOf("tier" to "gold")
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
