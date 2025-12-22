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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrderControllerIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc
) {

    @Test
    fun `create update and delete order`() {
        val createBody = """
            {
              "id": "order-1",
              "status": "PAID",
              "amount": 1000,
              "initial_amount": 1200,
              "discount_amount": 200,
              "metadata": { "channel": "web" },
              "customer": { "source_id": "cust-order", "email": "order@example.com" }
            }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody)
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.source_id").value("order-1"))
            .andExpect(jsonPath("$.customer_id").exists())

        mockMvc.perform(get("/v1/orders"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].source_id").value("order-1"))

        mockMvc.perform(get("/v1/orders/order-1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("PAID"))

        val updateBody = """
            {
              "id": "order-1",
              "status": "CANCELED",
              "discount_amount": 300
            }
        """.trimIndent()

        mockMvc.perform(
            put("/v1/orders/order-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("CANCELED"))
            .andExpect(jsonPath("$.discount_amount").value(300))

        mockMvc.perform(delete("/v1/orders/order-1"))
            .andExpect(status().isNoContent)

        mockMvc.perform(get("/v1/orders/order-1"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `rejects invalid order request`() {
        mockMvc.perform(
            post("/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        ).andExpect(status().isBadRequest)
    }
}
