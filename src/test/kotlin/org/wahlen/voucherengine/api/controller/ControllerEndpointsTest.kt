package org.wahlen.voucherengine.api.controller

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(
    controllers = [
        VoucherController::class,
        CustomerController::class,
        ValidationRuleController::class
    ]
)
class ControllerEndpointsTest @Autowired constructor(
    private val mockMvc: MockMvc
) {

    @Test
    fun `create voucher endpoint exists`() {
        val body = """
            { "code": "TEST", "type": "DISCOUNT_VOUCHER", "discount": { "type": "PERCENT", "percent_off": 10 }, "redemption": { "quantity": 100 } }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/vouchers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andExpect(status().isNotImplemented)
    }

    @Test
    fun `validate voucher endpoint exists`() {
        val body = """
            { "customer": { "source_id": "customer-123" }, "order": { "id": "order-1", "amount": 1000 } }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/vouchers/TEST/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andExpect(status().isNotImplemented)
    }

    @Test
    fun `stack validation endpoint exists`() {
        val body = """
            { "redeemables": [ { "object": "voucher", "id": "TEST" } ], "customer": { "source_id": "customer-123" } }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/validations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andExpect(status().isNotImplemented)
    }

    @Test
    fun `redeem endpoint exists`() {
        val body = """
            { "redeemables": [ { "object": "voucher", "id": "TEST" } ], "customer": { "source_id": "customer-123" }, "order": { "id": "order-1", "amount": 1500 } }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/redemptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andExpect(status().isNotImplemented)
    }

    @Test
    fun `create customer endpoint exists`() {
        val body = """
            { "source_id": "customer-123", "email": "alice@example.com" }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andExpect(status().isNotImplemented)
    }

    @Test
    fun `create validation rule endpoint exists`() {
        val body = """
            { "name": "One redemption per customer", "type": "redemptions", "conditions": { "redemptions": { "per_customer": 1 } } }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/validation-rules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andExpect(status().isNotImplemented)
    }

    @Test
    fun `assign validation rule endpoint exists`() {
        val body = """
            { "object": "voucher", "id": "TEST" }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/validation-rules/val_123/assignments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andExpect(status().isNotImplemented)
    }
}
