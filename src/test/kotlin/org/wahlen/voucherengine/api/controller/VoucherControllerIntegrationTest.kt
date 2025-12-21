package org.wahlen.voucherengine.api.controller

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
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class VoucherControllerIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc
) {

    @Test
    fun `create validate and redeem voucher via controller`() {
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

        val validateBody = """
            { "customer": { "source_id": "controller-customer" }, "order": { "id": "order-1", "amount": 1000 } }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/vouchers/$code/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validateBody)
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.valid").value(true))

        val redeemBody = """
            { "redeemables": [ { "object": "voucher", "id": "$code" } ], "customer": { "source_id": "controller-customer" }, "order": { "id": "order-1", "amount": 1500 } }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/redemptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(redeemBody)
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.result").value("success"))
    }
}
