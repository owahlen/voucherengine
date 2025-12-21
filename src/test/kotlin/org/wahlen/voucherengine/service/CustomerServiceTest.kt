package org.wahlen.voucherengine.service

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.api.dto.request.CustomerCreateRequest
import org.wahlen.voucherengine.api.dto.request.CustomerReferenceDto
import org.wahlen.voucherengine.persistence.repository.CustomerRepository
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CustomerServiceTest @Autowired constructor(
    private val customerService: CustomerService,
    private val customerRepository: CustomerRepository
) {

    @Test
    fun `upsert creates and updates customer`() {
        val created = customerService.upsert(
            CustomerCreateRequest(source_id = "cust-1", email = "a@example.com", name = "Alice")
        )
        assertNotNull(created.id)
        assertEquals("Alice", created.name)

        val updated = customerService.upsert(
            CustomerCreateRequest(source_id = "cust-1", email = "updated@example.com", name = "Alice Updated")
        )
        assertEquals("Alice Updated", updated.name)
        assertEquals("updated@example.com", updated.email)
    }

    @Test
    fun `ensureCustomer creates when missing`() {
        val ensured = customerService.ensureCustomer(CustomerReferenceDto(source_id = "cust-ensure", email = "ensure@example.com"))
        assertNotNull(ensured)
        val found = customerRepository.findBySourceId("cust-ensure")
        assertNotNull(found)
        assertEquals("ensure@example.com", found.email)
    }
}
