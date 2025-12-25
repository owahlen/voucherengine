package org.wahlen.voucherengine.api.controller

import tools.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.api.tenantJwt
import org.wahlen.voucherengine.config.S3IntegrationTest
import org.wahlen.voucherengine.persistence.model.async.AsyncJobStatus
import org.wahlen.voucherengine.persistence.model.customer.Customer
import org.wahlen.voucherengine.persistence.model.order.Order
import org.wahlen.voucherengine.persistence.model.tenant.Tenant
import org.wahlen.voucherengine.persistence.repository.AsyncJobRepository
import org.wahlen.voucherengine.persistence.repository.CustomerRepository
import org.wahlen.voucherengine.persistence.repository.OrderRepository
import org.wahlen.voucherengine.persistence.repository.TenantRepository
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit

@S3IntegrationTest
@AutoConfigureMockMvc
class OrderExportControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var asyncJobRepository: AsyncJobRepository

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Autowired
    private lateinit var customerRepository: CustomerRepository

    @Autowired
    private lateinit var tenantRepository: TenantRepository
    
    @Autowired
    private lateinit var redemptionRepository: org.wahlen.voucherengine.persistence.repository.RedemptionRepository
    
    @Autowired
    private lateinit var publicationRepository: org.wahlen.voucherengine.persistence.repository.PublicationRepository
    
    @Autowired
    private lateinit var voucherRepository: org.wahlen.voucherengine.persistence.repository.VoucherRepository
    
    @Autowired
    private lateinit var campaignRepository: org.wahlen.voucherengine.persistence.repository.CampaignRepository

    private lateinit var tenant: Tenant

    @BeforeEach
    @Transactional
    fun setup() {
        // Delete in correct order to avoid FK violations
        // Redemptions first (they reference vouchers and customers)
        redemptionRepository.deleteAll()
        // Publications next (they reference vouchers and customers)
        publicationRepository.deleteAll()
        // Vouchers (they reference campaigns)
        voucherRepository.deleteAll()
        // Campaigns
        campaignRepository.deleteAll()
        // Async jobs
        asyncJobRepository.deleteAll()
        // Orders (they reference customers)
        orderRepository.deleteAll()
        // Customers
        customerRepository.deleteAll()
        // Tenants (referenced by everything)
        tenantRepository.deleteAll()

        tenant = Tenant(name = "acme")
        tenant = tenantRepository.save(tenant)

        val customer = Customer(
            sourceId = "cust-001",
            name = "Test Customer",
            tenant = tenant
        )
        customerRepository.save(customer)

        // Create test orders
        repeat(3) { i ->
            val order = Order(
                sourceId = "order-${i + 1}",
                status = "PAID",
                amount = (100 + i * 10).toLong(),
                customer = customer,
                tenant = tenant
            )
            orderRepository.save(order)
        }
    }

    @Test
    fun `POST orders-export should create async export job for CSV`() {
        // Given
        val request = mapOf(
            "parameters" to mapOf(
                "fields" to listOf("id", "source_id", "status"),
                "filters" to mapOf("status" to "PAID")
            )
        )

        // When & Then
        val result = mockMvc.perform(
            post("/v1/orders/export")
                .header("tenant", "acme")
                .with(tenantJwt("acme"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.async_action_id").exists())
            .andExpect(jsonPath("$.message").value("Export job created"))
            .andReturn()

        val responseBody = objectMapper.readValue(result.response.contentAsString, Map::class.java)
        val jobId = UUID.fromString(responseBody["async_action_id"] as String)

        // Wait for job to complete
        await()
            .atMost(30, TimeUnit.SECONDS)
            .pollInterval(Duration.ofMillis(500))
            .untilAsserted {
                val job = asyncJobRepository.findById(jobId).orElseThrow()
                assertThat(job.status).isIn(AsyncJobStatus.COMPLETED, AsyncJobStatus.FAILED)
            }

        // Verify job completed successfully
        val job = asyncJobRepository.findById(jobId).orElseThrow()
        assertThat(job.status).isEqualTo(AsyncJobStatus.COMPLETED)
        assertThat(job.progress).isEqualTo(3)
        
        val jobResult = job.result as Map<*, *>
        assertThat(jobResult["format"]).isEqualTo("CSV")
        assertThat(jobResult["recordCount"]).isEqualTo(3)
        assertThat(jobResult["url"]).isNotNull()
    }

    @Test
    fun `POST orders-export should create async export job for JSON`() {
        // Given
        val request = mapOf(
            "parameters" to mapOf(
                "fields" to listOf("id", "source_id", "status", "amount"),
                "format" to "JSON"  // Format can be in parameters
            )
        )

        // When & Then
        val result = mockMvc.perform(
            post("/v1/orders/export")
                .header("tenant", "acme")
                .with(tenantJwt("acme"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.async_action_id").exists())
            .andExpect(jsonPath("$.message").value("Export job created"))
            .andReturn()

        val responseBody = objectMapper.readValue(result.response.contentAsString, Map::class.java)
        val jobId = UUID.fromString(responseBody["async_action_id"] as String)

        // Wait and verify
        await()
            .atMost(30, TimeUnit.SECONDS)
            .pollInterval(Duration.ofMillis(500))
            .untilAsserted {
                val job = asyncJobRepository.findById(jobId).orElseThrow()
                assertThat(job.status).isEqualTo(AsyncJobStatus.COMPLETED)
            }

        val job = asyncJobRepository.findById(jobId).orElseThrow()
        val jobResult = job.result as Map<*, *>
        assertThat(jobResult["format"]).isEqualTo("JSON")
    }

    @Test
    fun `POST orders-export should use CSV as default format`() {
        // Given - No format specified
        val request = mapOf<String, Any>()

        // When & Then
        mockMvc.perform(
            post("/v1/orders/export")
                .header("tenant", "acme")
                .with(tenantJwt("acme"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("Export job created"))
    }

    @Test
    fun `POST orders-export should require authentication`() {
        // Given
        val request = mapOf(
            "parameters" to mapOf(
                "fields" to listOf("id")
            )
        )

        // When & Then
        mockMvc.perform(
            post("/v1/orders/export")
                .header("tenant", "acme")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `POST orders-export should require valid tenant`() {
        // Given
        val request = mapOf("format" to "CSV")

        // When & Then
        mockMvc.perform(
            post("/v1/orders/export")
                .header("tenant", "invalid-tenant")
                .with(jwt().jwt { it.claim("tenants", listOf("invalid-tenant")).claim("realm_access", mapOf("roles" to listOf("ROLE_TENANT"))) })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().is4xxClientError)
    }
}
