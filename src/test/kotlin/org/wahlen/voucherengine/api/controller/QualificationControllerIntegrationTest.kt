package org.wahlen.voucherengine.api.controller

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
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
class QualificationControllerIntegrationTest @Autowired constructor(
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
    fun `qualifies vouchers for customer wallet`() {
        val voucherBody = """
            { "code": "QUAL-001", "type": "DISCOUNT_VOUCHER", "discount": { "type": "PERCENT", "percent_off": 10 }, "redemption": { "quantity": 1 }, "customer": { "source_id": "cust-qual" } }
        """.trimIndent()
        mockMvc.perform(
            post("/v1/vouchers")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(voucherBody)
        ).andExpect(status().isCreated)

        val qualificationBody = """
            {
              "scenario": "CUSTOMER_WALLET",
              "customer": { "source_id": "cust-qual" },
              "options": { "limit": 5 }
            }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/qualifications")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(qualificationBody)
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.redeemables.data[0].id").value("QUAL-001"))
    }

    @Test
    fun `qualifications use starting_after cursor`() {
        val firstVoucher = """
            { "code": "QUAL-OLD", "type": "DISCOUNT_VOUCHER", "discount": { "type": "PERCENT", "percent_off": 10 } }
        """.trimIndent()
        val secondVoucher = """
            { "code": "QUAL-NEW", "type": "DISCOUNT_VOUCHER", "discount": { "type": "PERCENT", "percent_off": 10 } }
        """.trimIndent()
        mockMvc.perform(
            post("/v1/vouchers")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(firstVoucher)
        ).andExpect(status().isCreated)
        mockMvc.perform(
            post("/v1/vouchers")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(secondVoucher)
        ).andExpect(status().isCreated)

        val firstPageBody = """
            { "options": { "limit": 1 } }
        """.trimIndent()
        val firstPage = mockMvc.perform(
            post("/v1/qualifications")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(firstPageBody)
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.redeemables.total").value(1))
            .andReturn()

        val cursor = com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(firstPage.response.contentAsString)
            .get("redeemables")
            .get("more_starting_after")
            .asText()

        val secondPageBody = """
            { "options": { "limit": 1, "starting_after": "$cursor" } }
        """.trimIndent()
        mockMvc.perform(
            post("/v1/qualifications")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(secondPageBody)
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.redeemables.total").value(1))
    }
}
