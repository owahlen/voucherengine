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
class RedemptionControllerIntegrationTest @Autowired constructor(
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
    fun `list get and rollback redemptions`() {
        val code = "ROLL-${UUID.randomUUID().toString().take(6)}"
        val voucherBody = """
            { "code": "$code", "type": "DISCOUNT_VOUCHER", "discount": { "type": "PERCENT", "percent_off": 10 }, "redemption": { "quantity": 2 } }
        """.trimIndent()
        mockMvc.perform(
            post("/v1/vouchers")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(voucherBody)
        ).andExpect(status().isCreated)

        val redemptionBody = """
            { "redeemables": [ { "object": "voucher", "id": "$code" } ], "customer": { "source_id": "rollback-user" }, "order": { "id": "o-1", "amount": 1000 } }
        """.trimIndent()
        val redemptionResult = mockMvc.perform(
            post("/v1/redemptions")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(redemptionBody)
        ).andExpect(status().isOk)
            .andReturn()

        val redemptionPayload = objectMapper.readValue(redemptionResult.response.contentAsString, Map::class.java)
        val redemptionId = ((redemptionPayload["redemptions"] as? List<*>)?.firstOrNull() as? Map<*, *>)?.get("id") as? String
            ?: error("Missing redemption id")

        mockMvc.perform(get("/v1/redemptions").header("tenant", tenantName).with(tenantJwt(tenantName)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.redemptions[0].id").exists())

        mockMvc.perform(get("/v1/redemptions/$redemptionId").header("tenant", tenantName).with(tenantJwt(tenantName)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(redemptionId))

        val rollbackBody = """
            { "reason": "Order canceled", "amount": 1000 }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/redemptions/$redemptionId/rollback")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(rollbackBody)
        ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.redemption_id").value(redemptionId))
    }

    @Test
    fun `redemptions return inapplicable redeemables when validation fails`() {
        val code = "FAIL-${UUID.randomUUID().toString().take(6)}"
        val voucherBody = """
            { "code": "$code", "type": "DISCOUNT_VOUCHER", "discount": { "type": "PERCENT", "percent_off": 10 } }
        """.trimIndent()
        mockMvc.perform(
            post("/v1/vouchers")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(voucherBody)
        ).andExpect(status().isCreated)

        val redemptionBody = """
            {
              "redeemables": [
                { "object": "voucher", "id": "$code" },
                { "object": "voucher", "id": "missing-code" }
              ],
              "customer": { "source_id": "redeem-user" },
              "order": { "id": "o-2", "amount": 1000 }
            }
        """.trimIndent()
        mockMvc.perform(
            post("/v1/redemptions")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(redemptionBody)
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.redemptions").isEmpty)
            .andExpect(jsonPath("$.inapplicable_redeemables[0].status").value("INAPPLICABLE"))
            .andExpect(jsonPath("$.inapplicable_redeemables[0].result.error.code").value("voucher_not_found"))
    }
}
