package org.wahlen.voucherengine.api.controller

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class VoucherListControllerIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc
) {
    @Test
    fun `list and delete vouchers`() {
        val createBody = """
            { "code": "LIST-001", "type": "DISCOUNT_VOUCHER", "discount": { "type": "PERCENT", "percent_off": 5 }, "redemption": { "quantity": 1 } }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/vouchers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody)
        ).andExpect(status().isCreated)

        mockMvc.perform(get("/v1/vouchers"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].code").exists())

        mockMvc.perform(delete("/v1/vouchers/LIST-001"))
            .andExpect(status().isNoContent)

        mockMvc.perform(get("/v1/vouchers/LIST-001"))
            .andExpect(status().isNotFound)
    }
}
