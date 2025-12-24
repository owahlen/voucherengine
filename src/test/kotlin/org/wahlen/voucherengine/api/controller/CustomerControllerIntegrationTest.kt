package org.wahlen.voucherengine.api.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.api.tenantJwt
import org.wahlen.voucherengine.config.IntegrationTest
import org.wahlen.voucherengine.persistence.model.tenant.Tenant
import org.wahlen.voucherengine.persistence.repository.TenantRepository

@IntegrationTest
@AutoConfigureMockMvc
@Transactional
class CustomerControllerIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
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
    fun `customer CRUD endpoints`() {
        val body = """
            { "source_id": "controller-customer-1", "email": "alice@example.com", "name": "Alice" }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/customers")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.sourceId").value("controller-customer-1"))

        mockMvc.perform(get("/v1/customers").header("tenant", tenantName).with(tenantJwt(tenantName)))
            .andExpect(status().isOk)

        mockMvc.perform(get("/v1/customers/controller-customer-1").header("tenant", tenantName).with(tenantJwt(tenantName)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value("alice@example.com"))

        val updateBody = """
            { "source_id": "controller-customer-1", "email": "updated@example.com", "name": "Alice Updated" }
        """.trimIndent()
        mockMvc.perform(
            put("/v1/customers/controller-customer-1")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody)
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value("updated@example.com"))

        mockMvc.perform(delete("/v1/customers/controller-customer-1").header("tenant", tenantName).with(tenantJwt(tenantName)))
            .andExpect(status().isNoContent)

        mockMvc.perform(get("/v1/customers/controller-customer-1").header("tenant", tenantName).with(tenantJwt(tenantName)))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `get customer segments`() {
        // First import required dependencies at top if not present
        val createCustomerBody = """
            { "source_id": "segment-customer", "email": "segment@example.com" }
        """.trimIndent()
        
        mockMvc.perform(
            post("/v1/customers")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createCustomerBody)
        ).andExpect(status().isOk)

        val createSegmentBody = """
            {
                "name": "Test Segment",
                "type": "static",
                "customers": ["segment-customer"]
            }
        """.trimIndent()
        
        mockMvc.perform(
            post("/v1/segments")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createSegmentBody)
        ).andExpect(status().isOk)

        mockMvc.perform(
            get("/v1/customers/segment-customer/segments")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.object").value("list"))
            .andExpect(jsonPath("$.data_ref").value("segments"))
            .andExpect(jsonPath("$.segments.length()").value(1))
            .andExpect(jsonPath("$.segments[0].name").value("Test Segment"))
    }

    @Test
    fun `creates tenant on first use when header is in tenants claim`() {
        val lazyTenant = "lazy-tenant"
        tenantRepository.findByName(lazyTenant)?.let { tenantRepository.delete(it) }

        val body = """
            { "source_id": "lazy-tenant-customer" }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/customers")
                .header("tenant", lazyTenant)
                .with(tenantJwt(lazyTenant))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andExpect(status().isOk)

        val createdTenant = tenantRepository.findByName(lazyTenant)
        assertThat(createdTenant).isNotNull()
    }
}
