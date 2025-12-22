package org.wahlen.voucherengine.api.controller

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.api.tenantJwt
import org.wahlen.voucherengine.persistence.model.tenant.Tenant
import org.wahlen.voucherengine.persistence.repository.TenantRepository
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PublicationControllerIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val tenantRepository: TenantRepository
) {
    private val objectMapper = ObjectMapper()
    private val tenantName = "test-tenant"

    @BeforeEach
    fun setUp() {
        if (tenantRepository.findByName(tenantName) == null) {
            tenantRepository.save(Tenant(name = tenantName))
        }
    }

    @Test
    fun `create publication from campaign with join once`() {
        val campaignName = "Campaign-${UUID.randomUUID().toString().take(6)}"
        val campaignBody = """
            { "name": "$campaignName", "type": "DISCOUNT_COUPONS", "mode": "STATIC", "code_pattern": "PUB-####" }
        """.trimIndent()

        val campaignResult = mockMvc.perform(
            post("/v1/campaigns")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(campaignBody)
        ).andExpect(status().isCreated)
            .andReturn()

        val campaignId = UUID.fromString(objectMapper.readTree(campaignResult.response.contentAsString).get("id").asString())
        val voucherCode = "PUB-${UUID.randomUUID().toString().take(6)}"
        val voucherBody = """
            { "code": "$voucherCode", "type": "DISCOUNT_VOUCHER", "discount": { "type": "PERCENT", "percent_off": 10 }, "redemption": { "quantity": 1 } }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/campaigns/$campaignId/vouchers")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(voucherBody)
        ).andExpect(status().isCreated)

        val customerSource = "customer-${UUID.randomUUID().toString().take(6)}"
        val publicationBody = """
            { "campaign": { "name": "$campaignName" }, "customer": { "source_id": "$customerSource", "email": "pub@example.com" }, "channel": "api" }
        """.trimIndent()

        val firstPublication = mockMvc.perform(
            post("/v1/publications?join_once=true")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(publicationBody)
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.object").value("publication"))
            .andExpect(jsonPath("$.voucher.code").value(voucherCode))
            .andReturn()

        val firstPublicationId = objectMapper.readTree(firstPublication.response.contentAsString).get("id").asString()
        val customerId = objectMapper.readTree(firstPublication.response.contentAsString).get("customer_id").asString()

        mockMvc.perform(
            post("/v1/publications?join_once=true")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(publicationBody)
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(firstPublicationId))
            .andExpect(jsonPath("$.voucher.code").value(voucherCode))

        mockMvc.perform(
            get("/v1/publications")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .param("customer", customerId)
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.publications[0].voucher.code").value(voucherCode))
    }

    @Test
    fun `create publication from specific voucher`() {
        val voucherCode = "DIRECT-${UUID.randomUUID().toString().take(6)}"
        val voucherBody = """
            { "code": "$voucherCode", "type": "DISCOUNT_VOUCHER", "discount": { "type": "PERCENT", "percent_off": 5 }, "redemption": { "quantity": 1 } }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/vouchers")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(voucherBody)
        ).andExpect(status().isCreated)

        val customerSource = "customer-${UUID.randomUUID().toString().take(6)}"
        val publicationBody = """
            { "voucher": "$voucherCode", "customer": { "source_id": "$customerSource", "email": "direct@example.com" }, "channel": "api" }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/publications")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(publicationBody)
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.voucher.code").value(voucherCode))
            .andExpect(jsonPath("$.customer.source_id").value(customerSource))
    }

    @Test
    fun `create publication via GET endpoint`() {
        val voucherCode = "GET-${UUID.randomUUID().toString().take(6)}"
        val voucherBody = """
            { "code": "$voucherCode", "type": "DISCOUNT_VOUCHER", "discount": { "type": "PERCENT", "percent_off": 7 }, "redemption": { "quantity": 1 } }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/vouchers")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(voucherBody)
        ).andExpect(status().isCreated)

        val customerSource = "customer-${UUID.randomUUID().toString().take(6)}"
        mockMvc.perform(
            get("/v1/publications/create")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .param("voucher", voucherCode)
                .param("customer[source_id]", customerSource)
                .param("channel", "api")
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.voucher.code").value(voucherCode))
            .andExpect(jsonPath("$.customer.source_id").value(customerSource))
    }

    @Test
    fun `create publication with multiple vouchers from campaign`() {
        val campaignName = "Campaign-${UUID.randomUUID().toString().take(6)}"
        val campaignBody = """
            { "name": "$campaignName", "type": "DISCOUNT_COUPONS", "mode": "STATIC", "code_pattern": "MPUB-####" }
        """.trimIndent()

        val campaignResult = mockMvc.perform(
            post("/v1/campaigns")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(campaignBody)
        ).andExpect(status().isCreated)
            .andReturn()

        val campaignId = UUID.fromString(objectMapper.readTree(campaignResult.response.contentAsString).get("id").asString())
        val voucherBody = { code: String ->
            """
            { "code": "$code", "type": "DISCOUNT_VOUCHER", "discount": { "type": "PERCENT", "percent_off": 15 }, "redemption": { "quantity": 1 } }
            """.trimIndent()
        }

        mockMvc.perform(
            post("/v1/campaigns/$campaignId/vouchers")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(voucherBody("MPUB-${UUID.randomUUID().toString().take(6)}"))
        ).andExpect(status().isCreated)

        mockMvc.perform(
            post("/v1/campaigns/$campaignId/vouchers")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(voucherBody("MPUB-${UUID.randomUUID().toString().take(6)}"))
        ).andExpect(status().isCreated)

        val customerSource = "customer-${UUID.randomUUID().toString().take(6)}"
        val publicationBody = """
            { "campaign": { "name": "$campaignName", "count": 2 }, "customer": { "source_id": "$customerSource", "email": "multi@example.com" }, "channel": "api" }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/publications")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(publicationBody)
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.vouchers").isArray)
            .andExpect(jsonPath("$.vouchers.length()").value(2))
            .andExpect(jsonPath("$.vouchers_id.length()").value(2))
    }
}
