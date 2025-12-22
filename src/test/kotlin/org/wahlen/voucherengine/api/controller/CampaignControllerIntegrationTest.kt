package org.wahlen.voucherengine.api.controller

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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CampaignControllerIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc
) {

    @Test
    fun `campaign CRUD and voucher issuance`() {
        val createBody = """
            { "name": "BF-2026", "type": "DISCOUNT_COUPONS", "mode": "AUTO_UPDATE", "description": "Black Friday" }
        """.trimIndent()

        val createResult = mockMvc.perform(
            post("/v1/campaigns")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody)
        ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.object").value("campaign"))
            .andReturn()

        val campaignId = objectMapper.readTree(createResult.response.contentAsString).get("id").asText()

        mockMvc.perform(get("/v1/campaigns/$campaignId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("BF-2026"))

        mockMvc.perform(get("/v1/campaigns"))
            .andExpect(status().isOk)

        val voucherBody = """
            { "code": "BF2026-0001", "type": "DISCOUNT_VOUCHER", "discount": { "type": "PERCENT", "percent_off": 10 }, "redemption": { "quantity": 1 } }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/campaigns/$campaignId/vouchers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(voucherBody)
        ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.code").value("BF2026-0001"))
    }

    companion object {
        private val objectMapper = tools.jackson.databind.ObjectMapper()
    }
}
