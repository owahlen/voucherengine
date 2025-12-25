package org.wahlen.voucherengine.api.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.api.tenantJwt
import org.wahlen.voucherengine.config.IntegrationTest
import org.wahlen.voucherengine.persistence.model.customer.Customer
import org.wahlen.voucherengine.persistence.model.segment.SegmentType
import org.wahlen.voucherengine.persistence.model.tenant.Tenant
import org.wahlen.voucherengine.persistence.repository.CustomerEventRepository
import org.wahlen.voucherengine.persistence.repository.CustomerRepository
import org.wahlen.voucherengine.persistence.repository.SegmentRepository
import org.wahlen.voucherengine.persistence.repository.TenantRepository

@IntegrationTest
@AutoConfigureMockMvc
@Transactional
class SegmentControllerIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val tenantRepository: TenantRepository,
    private val segmentRepository: SegmentRepository,
    private val customerRepository: CustomerRepository,
    private val customerEventRepository: CustomerEventRepository
) {
    private val tenantName = "test-tenant"
    private lateinit var tenant: Tenant

    @BeforeEach
    fun setUp() {
        tenant = tenantRepository.findByName(tenantName) ?: tenantRepository.save(Tenant(name = tenantName))
    }

    @Test
    fun `create static segment with customers by ID`() {
        val customer = customerRepository.save(Customer(
            sourceId = "cust-1",
            email = "test@example.com",
            tenant = tenant
        ))

        val body = """
            {
                "name": "VIP Customers",
                "type": "static",
                "customers": ["${customer.id}"]
            }
        """.trimIndent()

        val result = mockMvc.perform(
            post("/v1/segments")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("VIP Customers"))
            .andExpect(jsonPath("$.type").value("static"))
            .andReturn()

        val segmentId = result.response.contentAsString.let {
            it.substring(it.indexOf("\"id\":\"") + 6).substringBefore("\"")
        }

        val events = customerEventRepository.findAll()
        assertThat(events).anyMatch { event -> event.eventType == "customer.segment.entered" }
    }

    @Test
    fun `create static segment with customers by source_id`() {
        val customer = customerRepository.save(Customer(
            sourceId = "cust-source-1",
            email = "test@example.com",
            tenant = tenant
        ))

        val body = """
            {
                "name": "Segment by SourceId",
                "type": "static",
                "customers": ["cust-source-1"]
            }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/segments")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Segment by SourceId"))

        val segment = segmentRepository.findAllByTenant_Name(tenantName).first()
        assertThat(segment.customerIds).contains(customer.id)
    }

    @Test
    fun `create auto-update segment`() {
        val body = """
            {
                "name": "Auto Segment",
                "type": "auto-update"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/segments")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.type").value("auto-update"))
    }

    @Test
    fun `list segments`() {
        segmentRepository.save(org.wahlen.voucherengine.persistence.model.segment.Segment(
            name = "Segment 1",
            type = SegmentType.STATIC,
            tenant = tenant
        ))
        segmentRepository.save(org.wahlen.voucherengine.persistence.model.segment.Segment(
            name = "Segment 2",
            type = SegmentType.AUTO_UPDATE,
            tenant = tenant
        ))

        mockMvc.perform(
            get("/v1/segments")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.object").value("list"))
            .andExpect(jsonPath("$.data_ref").value("segments"))
            .andExpect(jsonPath("$.segments.length()").value(2))
    }

    @Test
    fun `get segment by ID`() {
        val segment = segmentRepository.save(org.wahlen.voucherengine.persistence.model.segment.Segment(
            name = "Test Segment",
            type = SegmentType.STATIC,
            tenant = tenant
        ))

        mockMvc.perform(
            get("/v1/segments/${segment.id}")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Test Segment"))
            .andExpect(jsonPath("$.id").value(segment.id.toString()))
    }

    @Test
    fun `delete segment`() {
        val segment = segmentRepository.save(org.wahlen.voucherengine.persistence.model.segment.Segment(
            name = "To Delete",
            type = SegmentType.STATIC,
            tenant = tenant
        ))

        mockMvc.perform(
            delete("/v1/segments/${segment.id}")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
        ).andExpect(status().isNoContent)

        mockMvc.perform(
            get("/v1/segments/${segment.id}")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
        ).andExpect(status().isNotFound)
    }

    @Test
    fun `tenant isolation - cannot access other tenant segments`() {
        val otherTenant = tenantRepository.save(Tenant(name = "other-tenant"))
        val segment = segmentRepository.save(org.wahlen.voucherengine.persistence.model.segment.Segment(
            name = "Other Tenant Segment",
            type = SegmentType.STATIC,
            tenant = otherTenant
        ))

        mockMvc.perform(
            get("/v1/segments/${segment.id}")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
        ).andExpect(status().isNotFound)
    }
}
