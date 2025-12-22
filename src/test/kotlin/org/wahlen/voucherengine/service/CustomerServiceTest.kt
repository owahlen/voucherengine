package org.wahlen.voucherengine.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.api.dto.request.CustomerCreateRequest
import org.wahlen.voucherengine.api.dto.request.CustomerReferenceDto
import org.wahlen.voucherengine.persistence.model.tenant.Tenant
import org.wahlen.voucherengine.persistence.repository.CustomerRepository
import org.wahlen.voucherengine.persistence.repository.TenantRepository
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CustomerServiceTest @Autowired constructor(
    private val customerService: CustomerService,
    private val customerRepository: CustomerRepository,
    private val tenantRepository: TenantRepository
) {
    private val tenantName = "test-tenant"

    @BeforeEach
    fun setUp() {
        if (tenantRepository.findByName(tenantName) == null) {
            tenantRepository.save(Tenant(name = tenantName))
        }
    }

    @Test
    fun `upsert creates and updates customer`() {
        val created = customerService.upsert(
            tenantName,
            CustomerCreateRequest(source_id = "cust-1", email = "a@example.com", name = "Alice")
        )
        assertNotNull(created.id)
        assertEquals("Alice", created.name)

        val updated = customerService.upsert(
            tenantName,
            CustomerCreateRequest(source_id = "cust-1", email = "updated@example.com", name = "Alice Updated")
        )
        assertEquals("Alice Updated", updated.name)
        assertEquals("updated@example.com", updated.email)
    }

    @Test
    fun `ensureCustomer creates when missing`() {
        val ensured = customerService.ensureCustomer(tenantName, CustomerReferenceDto(source_id = "cust-ensure", email = "ensure@example.com"))
        assertNotNull(ensured)
        val found = customerRepository.findBySourceIdAndTenantName("cust-ensure", tenantName)
        assertNotNull(found)
        assertEquals("ensure@example.com", found.email)
    }
}
