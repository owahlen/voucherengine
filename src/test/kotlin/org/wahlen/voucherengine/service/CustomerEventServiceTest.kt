package org.wahlen.voucherengine.service

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.config.IntegrationTest
import org.wahlen.voucherengine.persistence.model.campaign.Campaign
import org.wahlen.voucherengine.persistence.model.campaign.CampaignType
import org.wahlen.voucherengine.persistence.model.customer.Customer
import org.wahlen.voucherengine.persistence.model.event.CustomerEventType
import org.wahlen.voucherengine.persistence.model.event.EventCategory
import org.wahlen.voucherengine.persistence.model.tenant.Tenant
import org.wahlen.voucherengine.persistence.repository.*
import kotlin.test.*

@IntegrationTest
@Transactional
class CustomerEventServiceTest @Autowired constructor(
    private val customerEventService: CustomerEventService,
    private val customerEventRepository: CustomerEventRepository,
    private val customerRepository: CustomerRepository,
    private val campaignRepository: CampaignRepository,
    private val tenantRepository: TenantRepository
) {
    private val tenantName = "test-tenant"
    private lateinit var tenant: Tenant
    private lateinit var customer: Customer
    private lateinit var campaign: Campaign

    @BeforeEach
    fun setUp() {
        customerEventRepository.deleteAll()
        
        tenant = tenantRepository.findByName(tenantName) 
            ?: tenantRepository.save(Tenant(name = tenantName))
        
        customer = customerRepository.save(
            Customer(
                sourceId = "cust-event-1",
                email = "event@example.com",
                name = "Event Test Customer",
                tenant = tenant
            )
        )
        
        campaign = campaignRepository.save(
            Campaign(
                name = "Test Campaign",
                type = CampaignType.DISCOUNT_COUPONS,
                tenant = tenant
            )
        )
    }
    
    @AfterEach
    fun tearDown() {
        customerEventRepository.deleteAll()
    }

    @Test
    fun `logEvent creates event with denormalized customer snapshot`() {
        customerEventService.logEvent(
            tenantName, customer, CustomerEventType.CUSTOMER_CREATED,
            category = EventCategory.EFFECT, data = mapOf("custom_field" to "custom_value")
        )
        
        val events = customerEventRepository.findAll()
        assertEquals(1, events.size)
        assertEquals(CustomerEventType.CUSTOMER_CREATED, events[0].eventType)
        assertEquals("custom_value", events[0].data["custom_field"])
        
        val customerSnapshot = events[0].data["customer"] as? Map<*, *>
        assertNotNull(customerSnapshot)
        assertEquals(customer.id.toString(), customerSnapshot["id"])
    }

    @Test
    fun `logEvent with null customer does nothing`() {
        customerEventService.logEvent(tenantName, null, CustomerEventType.CUSTOMER_CREATED)
        assertEquals(0, customerEventRepository.findAll().size)
    }

    @Test
    fun `listCustomerActivity returns paginated events`() {
        repeat(5) {
            customerEventService.logEvent(tenantName, customer, CustomerEventType.REDEMPTION_SUCCEEDED, EventCategory.ACTION)
        }
        
        val pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending())
        val events = customerEventService.listCustomerActivity(tenantName, customer.id!!, pageable = pageable)
        assertEquals(5, events.totalElements)
    }
}
