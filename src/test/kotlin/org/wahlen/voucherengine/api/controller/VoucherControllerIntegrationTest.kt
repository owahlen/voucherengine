package org.wahlen.voucherengine.api.controller

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
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
import org.wahlen.voucherengine.persistence.repository.SessionLockRepository
import tools.jackson.databind.ObjectMapper
import java.util.UUID
import kotlin.test.assertEquals

@IntegrationTest
@AutoConfigureMockMvc
@Transactional
class VoucherControllerIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val tenantRepository: TenantRepository,
    private val sessionLockRepository: SessionLockRepository
) {
    private val tenantName = "test-tenant"

    @BeforeEach
    fun setUp() {
        if (tenantRepository.findByName(tenantName) == null) {
            tenantRepository.save(Tenant(name = tenantName))
        }
    }

    @Test
    fun `voucher CRUD via controller`() {
        val code = "INT-${UUID.randomUUID().toString().take(8)}"
        val createBody = """
            { "code": "$code", "type": "DISCOUNT_VOUCHER", "discount": { "type": "PERCENT", "percent_off": 10 }, "redemption": { "quantity": 1 } }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/vouchers")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody)
        ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.code").value(code))

        mockMvc.perform(get("/v1/vouchers/$code").header("tenant", tenantName).with(tenantJwt(tenantName)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(code))

        val validateBody = """
            { "customer": { "source_id": "controller-customer" }, "order": { "id": "order-1", "amount": 1000 } }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/vouchers/$code/validate")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(validateBody)
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.valid").value(true))

        // redeem once to consume limit
        val redeemBody = """
            { "redeemables": [ { "object": "voucher", "id": "$code" } ], "customer": { "source_id": "controller-customer" }, "order": { "id": "order-1", "amount": 1500 } }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/redemptions")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(redeemBody)
        ).andExpect(status().isOk)

        // next validation should fail with limit exceeded
        mockMvc.perform(
            post("/v1/vouchers/$code/validate")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(validateBody)
        ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("redemption_limit_exceeded"))

        val updateBody = """
            { "code": "$code", "type": "DISCOUNT_VOUCHER", "discount": { "type": "PERCENT", "percent_off": 15 }, "redemption": { "quantity": 2 } }
        """.trimIndent()

        mockMvc.perform(
            put("/v1/vouchers/$code")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody)
        ).andExpect(status().isOk)
    }

    @Test
    fun `enable and disable voucher`() {
        val code = "STATE-${UUID.randomUUID().toString().take(6)}"
        val createBody = """
            { "code": "$code", "type": "DISCOUNT_VOUCHER", "discount": { "type": "AMOUNT", "amount_off": 500 } }
        """.trimIndent()
        mockMvc.perform(
            post("/v1/vouchers")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody)
        ).andExpect(status().isCreated)

        mockMvc.perform(
            post("/v1/vouchers/$code/disable")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("INACTIVE"))

        mockMvc.perform(
            post("/v1/vouchers/$code/enable")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("ACTIVE"))
    }

    @Test
    fun `validate stack aggregates results`() {
        val code = "STACK-${UUID.randomUUID().toString().take(8)}"
        val createBody = """
            { "code": "$code", "type": "DISCOUNT_VOUCHER", "discount": { "type": "PERCENT", "percent_off": 5 }, "redemption": { "quantity": 5 } }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/vouchers")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody)
        ).andExpect(status().isCreated)

        val stackBody = """
            {
              "redeemables": [
                { "object": "voucher", "id": "$code" },
                { "object": "voucher", "id": "missing-code" }
              ],
              "customer": { "source_id": "stack-customer" },
              "order": { "id": "order-stack", "amount": 500 }
            }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/validations")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(stackBody)
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.valid").value(false))
            .andExpect(jsonPath("$.redeemables[0].status").value("APPLICABLE"))
            .andExpect(jsonPath("$.redeemables[1].status").value("INAPPLICABLE"))
            .andExpect(jsonPath("$.redeemables[1].result.error.code").value("voucher_not_found"))
    }

    @Test
    fun `qr and barcode endpoints return images`() {
        val code = "QR-${UUID.randomUUID().toString().take(6)}"
        val createBody = """
            { "code": "$code", "type": "DISCOUNT_VOUCHER", "discount": { "type": "PERCENT", "percent_off": 5 }, "redemption": { "quantity": 1 } }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/vouchers")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody)
        ).andExpect(status().isCreated)

        mockMvc.perform(get("/v1/vouchers/$code/qr").header("tenant", tenantName).with(tenantJwt(tenantName)))
            .andExpect(status().isOk)
            .andExpect { result -> assertEquals("image/png", result.response.contentType) }

        mockMvc.perform(get("/v1/vouchers/$code/barcode").header("tenant", tenantName).with(tenantJwt(tenantName)))
            .andExpect(status().isOk)
            .andExpect { result -> assertEquals("image/png", result.response.contentType) }
    }

    @Test
    fun `validations expand redeemable and category`() {
        val categoryBody = """
            { "name": "Promo Category" }
        """.trimIndent()
        val categoryResult = mockMvc.perform(
            post("/v1/categories")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(categoryBody)
        ).andExpect(status().isCreated)
            .andReturn()
        val categoryId = ObjectMapper()
            .readValue(categoryResult.response.contentAsString, Map::class.java)["id"] as? String
            ?: error("Missing category id")

        val code = "VAL-EXP-${UUID.randomUUID().toString().take(6)}"
        val voucherBody = """
            {
              "code": "$code",
              "type": "DISCOUNT_VOUCHER",
              "discount": { "type": "PERCENT", "percent_off": 10 },
              "metadata": { "source": "test" },
              "category_ids": ["$categoryId"]
            }
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
              "redeemables": [ { "object": "voucher", "id": "$code" } ],
              "options": { "expand": ["redeemable", "category"] }
            }
        """.trimIndent()
        mockMvc.perform(
            post("/v1/validations")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(validationBody)
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.redeemables[0].metadata.source").value("test"))
            .andExpect(jsonPath("$.redeemables[0].categories[0].name").value("Promo Category"))
    }

    @Test
    fun `validations lock session reports locked credits for gift vouchers`() {
        val code = "GIFT-${UUID.randomUUID().toString().take(6)}"
        val voucherBody = """
            { "code": "$code", "type": "GIFT_VOUCHER", "gift": { "amount": 10000 } }
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
              "redeemables": [ { "object": "gift_card", "id": "$code" } ],
              "session": { "type": "LOCK" }
            }
        """.trimIndent()
        val result = mockMvc.perform(
            post("/v1/validations")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(validationBody)
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.redeemables[0].result.gift.locked_credits").value(0))

        val sessionPayload = ObjectMapper()
            .readValue(result.andReturn().response.contentAsString, Map::class.java)
        val sessionKey = ((sessionPayload["session"] as? Map<*, *>)?.get("key") as? String)
            ?: error("Missing session key")
        val locks = sessionLockRepository.findAllByTenantNameAndSessionKey(tenantName, sessionKey)
        assertEquals(1, locks.size)
    }

    @Test
    fun `validations overwrite existing session locks by key`() {
        val codeOne = "LOCK-${UUID.randomUUID().toString().take(6)}"
        val codeTwo = "LOCK-${UUID.randomUUID().toString().take(6)}"
        val voucherOne = """{ "code": "$codeOne", "type": "DISCOUNT_VOUCHER", "discount": { "type": "PERCENT", "percent_off": 10 } }"""
        val voucherTwo = """{ "code": "$codeTwo", "type": "DISCOUNT_VOUCHER", "discount": { "type": "PERCENT", "percent_off": 15 } }"""
        mockMvc.perform(
            post("/v1/vouchers")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(voucherOne)
        ).andExpect(status().isCreated)
        mockMvc.perform(
            post("/v1/vouchers")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(voucherTwo)
        ).andExpect(status().isCreated)

        val sessionKey = "sess_override"
        val validationOne = """
            {
              "redeemables": [ { "object": "voucher", "id": "$codeOne" } ],
              "session": { "type": "LOCK", "key": "$sessionKey" }
            }
        """.trimIndent()
        mockMvc.perform(
            post("/v1/validations")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(validationOne)
        ).andExpect(status().isOk)

        var locks = sessionLockRepository.findAllByTenantNameAndSessionKey(tenantName, sessionKey)
        assertEquals(1, locks.size)
        assertEquals(codeOne, locks.first().redeemableId)

        val validationTwo = """
            {
              "redeemables": [ { "object": "voucher", "id": "$codeTwo" } ],
              "session": { "type": "LOCK", "key": "$sessionKey" }
            }
        """.trimIndent()
        mockMvc.perform(
            post("/v1/validations")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(validationTwo)
        ).andExpect(status().isOk)

        locks = sessionLockRepository.findAllByTenantNameAndSessionKey(tenantName, sessionKey)
        assertEquals(1, locks.size)
        assertEquals(codeTwo, locks.first().redeemableId)
    }

    @Test
    fun `validations enforce redeemable limits and uniqueness`() {
        val payload = (1..31).joinToString(",") { idx -> """{ "object": "voucher", "id": "CODE-$idx" }""" }
        val tooManyBody = """
            { "redeemables": [ $payload ] }
        """.trimIndent()
        mockMvc.perform(
            post("/v1/validations")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(tooManyBody)
        ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("redeemables limit exceeded"))

        val duplicateBody = """
            { "redeemables": [ { "object": "voucher", "id": "CODE-1" }, { "object": "voucher", "id": "CODE-1" } ] }
        """.trimIndent()
        mockMvc.perform(
            post("/v1/validations")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(duplicateBody)
        ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("redeemables must be unique"))
    }

    @Test
    fun `validations expand validation rules and report rule failures`() {
        val code = "RULE-${UUID.randomUUID().toString().take(6)}"
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

        val ruleBody = """
            {
              "name": "Order total above threshold",
              "type": "order",
              "rules": { "rules": { "1": { "name": "order.amount", "conditions": { "${'$'}gt": 1000 } } }, "logic": "1" }
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
        val ruleId = ObjectMapper()
            .readValue(ruleResult.response.contentAsString, Map::class.java)["id"] as? String
            ?: error("Missing rule id")

        val assignmentBody = """{ "object": "voucher", "id": "$code" }"""
        mockMvc.perform(
            post("/v1/validation-rules/$ruleId/assignments")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(assignmentBody)
        ).andExpect(status().isOk)

        val validationBody = """
            {
              "redeemables": [ { "object": "voucher", "id": "$code" } ],
              "order": { "amount": 100 },
              "options": { "expand": ["validation_rules"] }
            }
        """.trimIndent()
        mockMvc.perform(
            post("/v1/validations")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(validationBody)
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.valid").value(false))
            .andExpect(jsonPath("$.redeemables[0].status").value("INAPPLICABLE"))
            .andExpect(jsonPath("$.redeemables[0].result.error.code").value("rule_failed"))
            .andExpect(jsonPath("$.redeemables[0].validation_rule_id").value(ruleId))
            .andExpect(jsonPath("$.redeemables[0].validation_rules_assignments.object").value("list"))
            .andExpect(jsonPath("$.redeemables[0].validation_rules_assignments.total").value(1))
            .andExpect(jsonPath("$.redeemables[0].validation_rules_assignments.data[0].rule_id").value(ruleId))
    }

    @Test
    fun `validations enforce category limit per category`() {
        val categoryBody = """
            { "name": "Stack Category" }
        """.trimIndent()
        val categoryResult = mockMvc.perform(
            post("/v1/categories")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(categoryBody)
        ).andExpect(status().isCreated)
            .andReturn()
        val categoryId = ObjectMapper()
            .readValue(categoryResult.response.contentAsString, Map::class.java)["id"] as? String
            ?: error("Missing category id")

        val codeA = "STACK-A-${UUID.randomUUID().toString().take(6)}"
        val codeB = "STACK-B-${UUID.randomUUID().toString().take(6)}"
        val voucherBodyA = """
            { "code": "$codeA", "type": "DISCOUNT_VOUCHER", "discount": { "type": "PERCENT", "percent_off": 10 }, "category_ids": ["$categoryId"] }
        """.trimIndent()
        val voucherBodyB = """
            { "code": "$codeB", "type": "DISCOUNT_VOUCHER", "discount": { "type": "PERCENT", "percent_off": 10 }, "category_ids": ["$categoryId"] }
        """.trimIndent()
        mockMvc.perform(
            post("/v1/vouchers")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(voucherBodyA)
        ).andExpect(status().isCreated)
        mockMvc.perform(
            post("/v1/vouchers")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(voucherBodyB)
        ).andExpect(status().isCreated)

        val validationBody = """
            {
              "redeemables": [
                { "object": "voucher", "id": "$codeA" },
                { "object": "voucher", "id": "$codeB" }
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
            .andExpect(jsonPath("$.redeemables[1].status").value("SKIPPED"))
            .andExpect(jsonPath("$.redeemables[1].result.details.key").value("applicable_redeemables_per_category_limit_exceeded"))
    }

    @Test
    fun `invalid validity window rejected`() {
        val code = "BAD-${UUID.randomUUID().toString().take(6)}"
        val badBody = """
            {
              "code": "$code",
              "type": "DISCOUNT_VOUCHER",
              "discount": { "type": "PERCENT", "percent_off": 5 },
              "redemption": { "quantity": 1 },
              "validity_hours": { "daily": [ { "start_time": "12:00", "expiration_time": "10:00", "days_of_week": [1,2,3] } ] }
            }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/vouchers")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(badBody)
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `tenant header missing or mismatched is forbidden`() {
        val body = """
            { "code": "TENANT-TEST", "type": "DISCOUNT_VOUCHER", "discount": { "type": "PERCENT", "percent_off": 5 }, "redemption": { "quantity": 1 } }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/vouchers")
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andExpect(status().isForbidden)
            .andExpect(jsonPath("$.message").value("Tenant header required"))

        mockMvc.perform(
            post("/v1/vouchers")
                .header("tenant", "other-tenant")
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andExpect(status().isForbidden)
            .andExpect(jsonPath("$.message").value("Tenant not allowed"))
    }
}
