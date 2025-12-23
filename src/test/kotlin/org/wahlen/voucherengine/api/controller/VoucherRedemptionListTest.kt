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
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class VoucherRedemptionListTest @Autowired constructor(
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
    fun `get voucher redemptions returns empty list for unredeemed voucher`() {
        val code = "REDEEM-LIST-${UUID.randomUUID().toString().take(6)}"
        val createBody = """
            { "code": "$code", "type": "DISCOUNT_VOUCHER", "discount": { "type": "PERCENT", "percent_off": 10 }, "redemption": { "quantity": 5 } }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/vouchers")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody)
        ).andExpect(status().isCreated)

        mockMvc.perform(
            get("/v1/vouchers/$code/redemption")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.object").value("list"))
            .andExpect(jsonPath("$.data_ref").value("redemption_entries"))
            .andExpect(jsonPath("$.quantity").value(5))
            .andExpect(jsonPath("$.redeemed_quantity").value(0))
            .andExpect(jsonPath("$.total").value(0))
            .andExpect(jsonPath("$.redemption_entries").isArray)
            .andExpect(jsonPath("$.redemption_entries.length()").value(0))
    }

    @Test
    fun `get voucher redemptions returns all redemptions for voucher`() {
        val code = "MULTI-REDEEM-${UUID.randomUUID().toString().take(6)}"
        val createBody = """
            { "code": "$code", "type": "DISCOUNT_VOUCHER", "discount": { "type": "PERCENT", "percent_off": 10 }, "redemption": { "quantity": 5 } }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/vouchers")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody)
        ).andExpect(status().isCreated)

        // Redeem twice
        val redeemBody1 = """
            { "redeemables": [ { "object": "voucher", "id": "$code" } ], "customer": { "source_id": "customer-1" }, "order": { "id": "order-1", "amount": 1000 } }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/redemptions")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(redeemBody1)
        ).andExpect(status().isOk)

        val redeemBody2 = """
            { "redeemables": [ { "object": "voucher", "id": "$code" } ], "customer": { "source_id": "customer-2" }, "order": { "id": "order-2", "amount": 2000 } }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/redemptions")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(redeemBody2)
        ).andExpect(status().isOk)

        mockMvc.perform(
            get("/v1/vouchers/$code/redemption")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.quantity").value(5))
            .andExpect(jsonPath("$.redeemed_quantity").value(2))
            .andExpect(jsonPath("$.total").value(2))
            .andExpect(jsonPath("$.redemption_entries.length()").value(2))
            .andExpect(jsonPath("$.redemption_entries[0].object").value("redemption"))
            .andExpect(jsonPath("$.redemption_entries[0].result").value("SUCCESS"))
            .andExpect(jsonPath("$.redemption_entries[0].voucher.code").value(code))
            .andExpect(jsonPath("$.redemption_entries[1].result").value("SUCCESS"))
    }

    @Test
    fun `get voucher redemptions returns 404 for non-existent voucher`() {
        mockMvc.perform(
            get("/v1/vouchers/NON-EXISTENT/redemption")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
        ).andExpect(status().isNotFound)
    }
}
