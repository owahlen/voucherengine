package org.wahlen.voucherengine.api.controller

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.wahlen.voucherengine.config.IntegrationTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.api.tenantJwt
import org.wahlen.voucherengine.persistence.model.tenant.Tenant
import org.wahlen.voucherengine.persistence.repository.TenantRepository
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@IntegrationTest(
    properties = [
        "voucherengine.stacking-rules.redeemables-application-mode=PARTIAL",
        "voucherengine.stacking-rules.redeemables-sorting-rule=CATEGORY_HIERARCHY",
        "voucherengine.stacking-rules.exclusive-categories=exclusive",
        "voucherengine.stacking-rules.joint-categories=joint"
    ]
)
@AutoConfigureMockMvc

@Transactional
class ValidationStackingRulesIntegrationTest @Autowired constructor(
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
    fun `partial mode returns valid true when at least one redeemable applies`() {
        val code = "PART-${UUID.randomUUID().toString().take(6)}"
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

        val validationBody = """
            {
              "redeemables": [
                { "object": "voucher", "id": "$code" },
                { "object": "voucher", "id": "missing-code" }
              ]
            }
        """.trimIndent()
        mockMvc.perform(
            post("/v1/validations")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(validationBody)
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.valid").value(true))
            .andExpect(jsonPath("$.inapplicable_redeemables[0].status").value("INAPPLICABLE"))
    }

    @Test
    fun `category hierarchy sorting orders by category name`() {
        val categoryAlphaBody = """{ "name": "alpha" }"""
        val categoryBetaBody = """{ "name": "beta" }"""
        val categoryAlphaResult = mockMvc.perform(
            post("/v1/categories")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(categoryAlphaBody)
        ).andExpect(status().isCreated)
            .andReturn()
        val categoryBetaResult = mockMvc.perform(
            post("/v1/categories")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(categoryBetaBody)
        ).andExpect(status().isCreated)
            .andReturn()
        val categoryAlphaId = ObjectMapper()
            .readValue(categoryAlphaResult.response.contentAsString, Map::class.java)["id"] as? String
            ?: error("Missing category id")
        val categoryBetaId = ObjectMapper()
            .readValue(categoryBetaResult.response.contentAsString, Map::class.java)["id"] as? String
            ?: error("Missing category id")

        val codeAlpha = "ALPHA-${UUID.randomUUID().toString().take(6)}"
        val codeBeta = "BETA-${UUID.randomUUID().toString().take(6)}"
        val voucherAlphaBody = """
            { "code": "$codeAlpha", "type": "DISCOUNT_VOUCHER", "discount": { "type": "PERCENT", "percent_off": 10 }, "category_ids": ["$categoryAlphaId"] }
        """.trimIndent()
        val voucherBetaBody = """
            { "code": "$codeBeta", "type": "DISCOUNT_VOUCHER", "discount": { "type": "PERCENT", "percent_off": 10 }, "category_ids": ["$categoryBetaId"] }
        """.trimIndent()
        mockMvc.perform(
            post("/v1/vouchers")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(voucherAlphaBody)
        ).andExpect(status().isCreated)
        mockMvc.perform(
            post("/v1/vouchers")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(voucherBetaBody)
        ).andExpect(status().isCreated)

        val validationBody = """
            {
              "redeemables": [
                { "object": "voucher", "id": "$codeBeta" },
                { "object": "voucher", "id": "$codeAlpha" }
              ]
            }
        """.trimIndent()
        mockMvc.perform(
            post("/v1/validations")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(validationBody)
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.redeemables[0].id").value(codeAlpha))
            .andExpect(jsonPath("$.redeemables[1].id").value(codeBeta))
    }

    @Test
    fun `promotion redeemables are skipped in partial mode`() {
        val code = "PROMO-${UUID.randomUUID().toString().take(6)}"
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

        val validationBody = """
            {
              "redeemables": [
                { "object": "promotion_tier", "id": "tier-1" },
                { "object": "voucher", "id": "$code" }
              ]
            }
        """.trimIndent()
        mockMvc.perform(
            post("/v1/validations")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(validationBody)
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.valid").value(true))
            .andExpect(jsonPath("$.redeemables[0].status").value("SKIPPED"))
            .andExpect(jsonPath("$.redeemables[0].object").value("promotion_tier"))
            .andExpect(jsonPath("$.redeemables[0].result.details.key").value("promotion_not_supported"))
    }

    @Test
    fun `exclusive category blocks non-joint categories`() {
        tenantRepository.findByName(tenantName) ?: tenantRepository.save(Tenant(name = tenantName))
        val exclusiveCategoryResult = mockMvc.perform(
            post("/v1/categories")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{ "name": "exclusive" }""")
        ).andExpect(status().isCreated)
            .andReturn()
        val jointCategoryResult = mockMvc.perform(
            post("/v1/categories")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{ "name": "joint" }""")
        ).andExpect(status().isCreated)
            .andReturn()
        val regularCategoryResult = mockMvc.perform(
            post("/v1/categories")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{ "name": "regular" }""")
        ).andExpect(status().isCreated)
            .andReturn()

        val exclusiveCategoryId = ObjectMapper()
            .readValue(exclusiveCategoryResult.response.contentAsString, Map::class.java)["id"] as? String
            ?: error("Missing category id")
        val jointCategoryId = ObjectMapper()
            .readValue(jointCategoryResult.response.contentAsString, Map::class.java)["id"] as? String
            ?: error("Missing category id")
        val regularCategoryId = ObjectMapper()
            .readValue(regularCategoryResult.response.contentAsString, Map::class.java)["id"] as? String
            ?: error("Missing category id")

        val codeExclusive = "EXC-${UUID.randomUUID().toString().take(6)}"
        val codeJoint = "JNT-${UUID.randomUUID().toString().take(6)}"
        val codeRegular = "REG-${UUID.randomUUID().toString().take(6)}"
        val voucherExclusiveBody = """
            { "code": "$codeExclusive", "type": "DISCOUNT_VOUCHER", "discount": { "type": "PERCENT", "percent_off": 10 }, "category_ids": ["$exclusiveCategoryId"] }
        """.trimIndent()
        val voucherJointBody = """
            { "code": "$codeJoint", "type": "DISCOUNT_VOUCHER", "discount": { "type": "PERCENT", "percent_off": 10 }, "category_ids": ["$jointCategoryId"] }
        """.trimIndent()
        val voucherRegularBody = """
            { "code": "$codeRegular", "type": "DISCOUNT_VOUCHER", "discount": { "type": "PERCENT", "percent_off": 10 }, "category_ids": ["$regularCategoryId"] }
        """.trimIndent()
        mockMvc.perform(
            post("/v1/vouchers")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(voucherExclusiveBody)
        ).andExpect(status().isCreated)
        mockMvc.perform(
            post("/v1/vouchers")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(voucherJointBody)
        ).andExpect(status().isCreated)
        mockMvc.perform(
            post("/v1/vouchers")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(voucherRegularBody)
        ).andExpect(status().isCreated)

        val validationBody = """
            {
              "redeemables": [
                { "object": "voucher", "id": "$codeExclusive" },
                { "object": "voucher", "id": "$codeJoint" },
                { "object": "voucher", "id": "$codeRegular" }
              ]
            }
        """.trimIndent()
        mockMvc.perform(
            post("/v1/validations")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(validationBody)
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.redeemables[0].status").value("APPLICABLE"))
            .andExpect(jsonPath("$.redeemables[1].status").value("APPLICABLE"))
            .andExpect(jsonPath("$.redeemables[2].status").value("SKIPPED"))
            .andExpect(jsonPath("$.redeemables[2].result.details.key").value("exclusion_rules_not_met"))
    }
}
