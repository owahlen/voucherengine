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

        val updateBody = """
            { "code": "$code", "type": "DISCOUNT_VOUCHER", "discount": { "type": "PERCENT", "percent_off": 15 }, "redemption": { "quantity": 2 } }
        """.trimIndent()

        mockMvc.perform(
            put("/v1/vouchers/$code")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody)
        ).andExpect(status().isOk)
    }
}
