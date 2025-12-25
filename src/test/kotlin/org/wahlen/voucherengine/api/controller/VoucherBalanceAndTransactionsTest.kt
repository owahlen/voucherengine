package org.wahlen.voucherengine.api.controller

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.wahlen.voucherengine.config.IntegrationTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.api.tenantJwt
import org.wahlen.voucherengine.persistence.model.tenant.Tenant
import org.wahlen.voucherengine.persistence.repository.TenantRepository
import org.wahlen.voucherengine.persistence.repository.SessionLockRepository
import tools.jackson.databind.ObjectMapper
import java.util.UUID
import kotlin.test.assertEquals

@IntegrationTest
@AutoConfigureMockMvc

@Transactional
class VoucherBalanceAndTransactionsTest @Autowired constructor(
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
    fun `adjust gift voucher balance and list transactions`() {
        val code = "GIFT-BAL-${UUID.randomUUID().toString().take(6)}"
        val createBody = """
            { "code": "$code", "type": "GIFT_VOUCHER", "gift": { "amount": 10000 } }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/vouchers")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody)
        ).andExpect(status().isOk)

        val addBalanceBody = """
            { "amount": 5000, "reason": "Customer credit" }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/vouchers/$code/balance")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(addBalanceBody)
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.amount").value(5000))
            .andExpect(jsonPath("$.balance").value(15000))
            .andExpect(jsonPath("$.type").value("gift_voucher"))

        val removeBalanceBody = """
            { "amount": -2000, "reason": "Adjustment" }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/vouchers/$code/balance")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(removeBalanceBody)
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.amount").value(-2000))
            .andExpect(jsonPath("$.balance").value(13000))

        mockMvc.perform(
            get("/v1/vouchers/$code/transactions")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.object").value("list"))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].type").value("CREDITS_REMOVAL"))
            .andExpect(jsonPath("$.data[0].amount").value(-2000))
            .andExpect(jsonPath("$.data[0].details.balance.balance").value(13000))
            .andExpect(jsonPath("$.data[1].type").value("CREDITS_ADDITION"))
            .andExpect(jsonPath("$.data[1].amount").value(5000))
    }

    @Test
    fun `adjust loyalty card balance`() {
        val code = "LOYALTY-BAL-${UUID.randomUUID().toString().take(6)}"
        val createBody = """
            { "code": "$code", "type": "LOYALTY_CARD", "loyalty_card": { "points": 1000 } }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/vouchers")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody)
        ).andExpect(status().isOk)

        val addPointsBody = """
            { "amount": 500, "source_id": "order-123", "reason": "Purchase bonus" }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/vouchers/$code/balance")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(addPointsBody)
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.amount").value(500))
            .andExpect(jsonPath("$.balance").value(1500))
            .andExpect(jsonPath("$.type").value("loyalty_card"))

        mockMvc.perform(
            get("/v1/vouchers/$code/transactions")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].type").value("POINTS_ADDITION"))
            .andExpect(jsonPath("$.data[0].source_id").value("order-123"))
            .andExpect(jsonPath("$.data[0].reason").value("Purchase bonus"))
    }

    @Test
    fun `balance adjustment rejects insufficient balance`() {
        val code = "GIFT-INSUF-${UUID.randomUUID().toString().take(6)}"
        val createBody = """
            { "code": "$code", "type": "GIFT_VOUCHER", "gift": { "amount": 1000 } }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/vouchers")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody)
        ).andExpect(status().isOk)

        val removeBody = """
            { "amount": -2000 }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/vouchers/$code/balance")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(removeBody)
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `balance adjustment rejects discount vouchers`() {
        val code = "DISC-${UUID.randomUUID().toString().take(6)}"
        val createBody = """
            { "code": "$code", "type": "DISCOUNT_VOUCHER", "discount": { "type": "PERCENT", "percent_off": 10 } }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/vouchers")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody)
        ).andExpect(status().isOk)

        val balanceBody = """
            { "amount": 1000 }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/vouchers/$code/balance")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(balanceBody)
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `release validation session`() {
        val code = "SESSION-${UUID.randomUUID().toString().take(6)}"
        val createBody = """
            { "code": "$code", "type": "DISCOUNT_VOUCHER", "discount": { "type": "PERCENT", "percent_off": 10 } }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/vouchers")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody)
        ).andExpect(status().isOk)

        val sessionKey = "sess_test_key"
        val validationBody = """
            {
              "redeemables": [ { "object": "voucher", "id": "$code" } ],
              "session": { "type": "LOCK", "key": "$sessionKey" }
            }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/validations")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(validationBody)
        ).andExpect(status().isOk)

        var locks = sessionLockRepository.findAllByTenantNameAndSessionKey(tenantName, sessionKey)
        assertEquals(1, locks.size)

        mockMvc.perform(
            delete("/v1/vouchers/$code/sessions/$sessionKey")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
        ).andExpect(status().isNoContent)

        locks = sessionLockRepository.findAllByTenantNameAndSessionKey(tenantName, sessionKey)
        assertEquals(0, locks.size)
    }

    @Test
    fun `transactions endpoint returns 404 for non-existent voucher`() {
        mockMvc.perform(
            get("/v1/vouchers/NON-EXISTENT/transactions")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
        ).andExpect(status().isNotFound)
    }

    @Test
    fun `balance endpoint returns 404 for non-existent voucher`() {
        val balanceBody = """
            { "amount": 1000 }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/vouchers/NON-EXISTENT/balance")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(balanceBody)
        ).andExpect(status().isNotFound)
    }
}
