package org.wahlen.voucherengine.api.controller

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.api.tenantJwt
import org.wahlen.voucherengine.persistence.model.tenant.Tenant
import org.wahlen.voucherengine.persistence.repository.TenantRepository

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CampaignControllerIntegrationTest @Autowired constructor(
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
    fun `campaign CRUD and voucher issuance`() {
        val createBody = """
            { "name": "BF-2026", "type": "DISCOUNT_COUPONS", "mode": "AUTO_UPDATE", "description": "Black Friday" }
        """.trimIndent()

        val createResult = mockMvc.perform(
            post("/v1/campaigns")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody)
        ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.object").value("campaign"))
            .andReturn()

        val campaignId = objectMapper.readTree(createResult.response.contentAsString).get("id").asString()

        mockMvc.perform(get("/v1/campaigns/$campaignId").header("tenant", tenantName).with(tenantJwt(tenantName)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("BF-2026"))

        mockMvc.perform(get("/v1/campaigns").header("tenant", tenantName).with(tenantJwt(tenantName)))
            .andExpect(status().isOk)

        val voucherBody = """
            { "code": "BF2026-0001", "type": "DISCOUNT_VOUCHER", "discount": { "type": "PERCENT", "percent_off": 10 }, "redemption": { "quantity": 1 } }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/campaigns/$campaignId/vouchers")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(voucherBody)
        ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.code").value("BF2026-0001"))

        mockMvc.perform(get("/v1/campaigns/$campaignId/vouchers").header("tenant", tenantName).with(tenantJwt(tenantName)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].code").value("BF2026-0001"))

        val updateBody = """{ "name": "BF-UPDATED", "type": "DISCOUNT_COUPONS", "mode": "STATIC" }"""
        mockMvc.perform(
            put("/v1/campaigns/$campaignId")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody)
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("BF-UPDATED"))

        mockMvc.perform(delete("/v1/campaigns/$campaignId").header("tenant", tenantName).with(tenantJwt(tenantName)))
            .andExpect(status().isNoContent)

        mockMvc.perform(get("/v1/campaigns/$campaignId").header("tenant", tenantName).with(tenantJwt(tenantName)))
            .andExpect(status().isNotFound)
    }

    companion object {
        private val objectMapper = tools.jackson.databind.ObjectMapper()
    }
}
