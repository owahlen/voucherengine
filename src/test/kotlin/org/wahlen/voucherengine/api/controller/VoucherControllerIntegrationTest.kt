package org.wahlen.voucherengine.api.controller

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import kotlin.test.assertEquals

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class VoucherControllerIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc
) {

    @Test
    fun `voucher CRUD via controller`() {
        val code = "INT-${UUID.randomUUID().toString().take(8)}"
        val createBody = """
            { "code": "$code", "type": "DISCOUNT_VOUCHER", "discount": { "type": "PERCENT", "percent_off": 10 }, "redemption": { "quantity": 1 } }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/vouchers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody)
        ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.code").value(code))

        mockMvc.perform(get("/v1/vouchers/$code"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(code))

        val validateBody = """
            { "customer": { "source_id": "controller-customer" }, "order": { "id": "order-1", "amount": 1000 } }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/vouchers/$code/validate")
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
                .contentType(MediaType.APPLICATION_JSON)
                .content(redeemBody)
        ).andExpect(status().isOk)

        // next validation should fail with limit exceeded
        mockMvc.perform(
            post("/v1/vouchers/$code/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validateBody)
        ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("redemption_limit_exceeded"))

        val updateBody = """
            { "code": "$code", "type": "DISCOUNT_VOUCHER", "discount": { "type": "PERCENT", "percent_off": 15 }, "redemption": { "quantity": 2 } }
        """.trimIndent()

        mockMvc.perform(
            put("/v1/vouchers/$code")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody)
        ).andExpect(status().isOk)
    }

    @Test
    fun `validate stack aggregates results`() {
        val code = "STACK-${UUID.randomUUID().toString().take(8)}"
        val createBody = """
            { "code": "$code", "type": "DISCOUNT_VOUCHER", "discount": { "type": "PERCENT", "percent_off": 5 }, "redemption": { "quantity": 5 } }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/vouchers")
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
                .contentType(MediaType.APPLICATION_JSON)
                .content(stackBody)
        ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.validations[0].valid").value(true))
            .andExpect(jsonPath("$.validations[1].valid").value(false))
            .andExpect(jsonPath("$.validations[1].error.code").value("voucher_not_found"))
    }

    @Test
    fun `qr and barcode endpoints return images`() {
        val code = "QR-${UUID.randomUUID().toString().take(6)}"
        val createBody = """
            { "code": "$code", "type": "DISCOUNT_VOUCHER", "discount": { "type": "PERCENT", "percent_off": 5 }, "redemption": { "quantity": 1 } }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/vouchers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody)
        ).andExpect(status().isCreated)

        mockMvc.perform(get("/v1/vouchers/$code/qr"))
            .andExpect(status().isOk)
            .andExpect { result -> assertEquals("image/png", result.response.contentType) }

        mockMvc.perform(get("/v1/vouchers/$code/barcode"))
            .andExpect(status().isOk)
            .andExpect { result -> assertEquals("image/png", result.response.contentType) }
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
                .contentType(MediaType.APPLICATION_JSON)
                .content(badBody)
        ).andExpect(status().isBadRequest)
    }
}
