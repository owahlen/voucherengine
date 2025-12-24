package org.wahlen.voucherengine.persistence

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.config.IntegrationTest
import org.wahlen.voucherengine.persistence.model.customer.Customer
import org.wahlen.voucherengine.persistence.model.segment.Segment
import org.wahlen.voucherengine.persistence.model.tenant.Tenant
import org.wahlen.voucherengine.persistence.repository.CustomerRepository
import org.wahlen.voucherengine.persistence.repository.SegmentRepository
import org.wahlen.voucherengine.persistence.repository.TenantRepository
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@IntegrationTest
@Transactional
class SegmentRepositoryTest @Autowired constructor(
    private val segmentRepository: SegmentRepository,
    private val customerRepository: CustomerRepository,
    private val tenantRepository: TenantRepository
) {
    private val tenantName = "test-tenant"
    private lateinit var tenant: Tenant
    private lateinit var customer1: Customer
    private lateinit var customer2: Customer

    @BeforeEach
    fun setUp() {
        tenant = tenantRepository.findByName(tenantName) ?: tenantRepository.save(Tenant(name = tenantName))
        
        customer1 = customerRepository.save(Customer(
            sourceId = "cust-001",
            name = "Customer One",
            tenant = tenant
        ))
        
        customer2 = customerRepository.save(Customer(
            sourceId = "cust-002",
            name = "Customer Two",
            tenant = tenant
        ))
    }

    @Test
    fun `segment CRUD persists customer IDs`() {
        val segment = Segment(
            name = "VIP Customers",
            customerIds = mutableSetOf(customer1.id!!, customer2.id!!),
            tenant = tenant
        )

        val saved = segmentRepository.save(segment)
        assertNotNull(saved.id)
        assertEquals("VIP Customers", saved.name)
        assertEquals(2, saved.customerIds.size)
        assertTrue(saved.customerIds.contains(customer1.id!!))
        assertTrue(saved.customerIds.contains(customer2.id!!))
    }

    @Test
    fun `findAllByTenant_Name returns all segments for tenant`() {
        segmentRepository.save(Segment(
            name = "Segment A",
            customerIds = mutableSetOf(customer1.id!!),
            tenant = tenant
        ))
        
        segmentRepository.save(Segment(
            name = "Segment B",
            customerIds = mutableSetOf(customer2.id!!),
            tenant = tenant
        ))

        val segments = segmentRepository.findAllByTenant_Name(tenantName)
        assertTrue(segments.size >= 2)
    }

    @Test
    fun `findByIdAndTenant_Name returns segment when found`() {
        val segment = segmentRepository.save(Segment(
            name = "Test Segment",
            customerIds = mutableSetOf(customer1.id!!),
            tenant = tenant
        ))

        val found = segmentRepository.findByIdAndTenant_Name(segment.id!!, tenantName)
        assertNotNull(found)
        assertEquals("Test Segment", found.name)
    }

    @Test
    fun `findAllByTenant_NameAndCustomerIdsContaining returns segments with specific customer`() {
        val segment1 = segmentRepository.save(Segment(
            name = "Segment With Customer 1",
            customerIds = mutableSetOf(customer1.id!!),
            tenant = tenant
        ))
        
        val segment2 = segmentRepository.save(Segment(
            name = "Segment With Both",
            customerIds = mutableSetOf(customer1.id!!, customer2.id!!),
            tenant = tenant
        ))
        
        segmentRepository.save(Segment(
            name = "Segment With Customer 2 Only",
            customerIds = mutableSetOf(customer2.id!!),
            tenant = tenant
        ))

        val segmentsWithCustomer1 = segmentRepository.findAllByTenant_NameAndCustomerIdsContaining(
            tenantName, 
            customer1.id!!
        )
        
        assertTrue(segmentsWithCustomer1.size >= 2)
        assertTrue(segmentsWithCustomer1.any { it.id == segment1.id })
        assertTrue(segmentsWithCustomer1.any { it.id == segment2.id })
    }
}
