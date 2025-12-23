package org.wahlen.voucherengine.api.controller

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.wahlen.voucherengine.config.IntegrationTest
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
import tools.jackson.databind.ObjectMapper

@IntegrationTest
@AutoConfigureMockMvc

@Transactional
class QualificationControllerIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val tenantRepository: TenantRepository
) {
    private val tenantName = "test-tenant"
    private val objectMapper = ObjectMapper()

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

        val cursorPayload = com.fasterxml.jackson.databind.ObjectMapper()
            .readValue(firstPage.response.contentAsString, Map::class.java)
        val cursor = ((cursorPayload["redeemables"] as? Map<*, *>)?.get("more_starting_after") as? String)
            ?: error("Missing qualification id")

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

    @Test
    fun `qualifications support filters and sorting`() {
        val voucherLow = """
            { "code": "QUAL-LOW", "type": "DISCOUNT_VOUCHER", "discount": { "type": "PERCENT", "percent_off": 5 } }
        """.trimIndent()
        val voucherHigh = """
            { "code": "QUAL-HIGH", "type": "DISCOUNT_VOUCHER", "discount": { "type": "PERCENT", "percent_off": 20 } }
        """.trimIndent()
        mockMvc.perform(
            post("/v1/vouchers")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(voucherLow)
        ).andExpect(status().isCreated)
        mockMvc.perform(
            post("/v1/vouchers")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(voucherHigh)
        ).andExpect(status().isCreated)

        val sortingBody = """
            {
              "order": { "amount": 1000 },
              "options": { "limit": 1, "sorting_rule": "BEST_DEAL" }
            }
        """.trimIndent()
        mockMvc.perform(
            post("/v1/qualifications")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(sortingBody)
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.redeemables.data[0].id").value("QUAL-HIGH"))

        val filterBody = """
            {
              "options": {
                "filters": {
                  "code": { "conditions": { "${'$'}is": ["QUAL-LOW"] } }
                }
              }
            }
        """.trimIndent()
        mockMvc.perform(
            post("/v1/qualifications")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(filterBody)
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.redeemables.data[0].id").value("QUAL-LOW"))
    }

    @Test
    fun `qualifications expand validation rules`() {
        val voucherBody = """
            { "code": "QUAL-RULE", "type": "DISCOUNT_VOUCHER", "discount": { "type": "PERCENT", "percent_off": 10 } }
        """.trimIndent()
        mockMvc.perform(
            post("/v1/vouchers")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(voucherBody)
        ).andExpect(status().isCreated)

        val ruleBody = """
            {
              "name": "Redemption total rule",
              "type": "redemptions",
              "conditions": {
                "rules": {
                  "1": { "name": "redemptions.count.total", "conditions": { "${'$'}lt": 5 } }
                },
                "logic": "1"
              }
            }
        """.trimIndent()
        val ruleResult = mockMvc.perform(
            post("/v1/validation-rules")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(ruleBody)
        ).andExpect(status().isCreated)
            .andReturn()

        val ruleId = objectMapper.readTree(ruleResult.response.contentAsString).get("id").asString()
        val assignBody = """
            { "object": "voucher", "id": "QUAL-RULE" }
        """.trimIndent()
        mockMvc.perform(
            post("/v1/validation-rules/$ruleId/assignments")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(assignBody)
        ).andExpect(status().isOk)

        val qualificationBody = """
            {
              "customer": { "source_id": "cust-rule" },
              "options": { "expand": ["validation_rules"] }
            }
        """.trimIndent()
        mockMvc.perform(
            post("/v1/qualifications")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(qualificationBody)
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.redeemables.data[0].validation_rules_assignments.object").value("list"))
            .andExpect(jsonPath("$.redeemables.data[0].validation_rules_assignments.total").value(1))
            .andExpect(jsonPath("$.redeemables.data[0].validation_rules_assignments.data[0].rule_id").value(ruleId))
    }

    @Test
    fun `audience only ignores order rules`() {
        val voucherBody = """
            { "code": "QUAL-AUDIENCE", "type": "DISCOUNT_VOUCHER", "discount": { "type": "PERCENT", "percent_off": 10 } }
        """.trimIndent()
        mockMvc.perform(
            post("/v1/vouchers")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(voucherBody)
        ).andExpect(status().isCreated)

        val ruleBody = """
            {
              "name": "Order amount rule",
              "type": "order",
              "conditions": {
                "rules": {
                  "1": { "name": "order.amount", "conditions": { "${'$'}gte": 2000 } }
                },
                "logic": "1"
              }
            }
        """.trimIndent()
        val ruleResult = mockMvc.perform(
            post("/v1/validation-rules")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(ruleBody)
        ).andExpect(status().isCreated)
            .andReturn()

        val ruleId = objectMapper.readTree(ruleResult.response.contentAsString).get("id").asString()
        val assignBody = """
            { "object": "voucher", "id": "QUAL-AUDIENCE" }
        """.trimIndent()
        mockMvc.perform(
            post("/v1/validation-rules/$ruleId/assignments")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(assignBody)
        ).andExpect(status().isOk)

        val baseBody = """
            { "customer": { "source_id": "cust-audience" } }
        """.trimIndent()
        mockMvc.perform(
            post("/v1/qualifications")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(baseBody)
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.redeemables.total").value(0))

        val audienceBody = """
            { "scenario": "AUDIENCE_ONLY", "customer": { "source_id": "cust-audience" } }
        """.trimIndent()
        mockMvc.perform(
            post("/v1/qualifications")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(audienceBody)
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.redeemables.data[0].id").value("QUAL-AUDIENCE"))
    }
}
