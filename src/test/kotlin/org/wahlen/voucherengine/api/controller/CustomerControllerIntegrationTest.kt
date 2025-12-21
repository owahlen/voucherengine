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
class CustomerControllerIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc
) {

    @Test
    fun `customer CRUD endpoints`() {
        val body = """
            { "source_id": "controller-customer-1", "email": "alice@example.com", "name": "Alice" }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.sourceId").value("controller-customer-1"))

        mockMvc.perform(get("/v1/customers"))
            .andExpect(status().isOk)

        mockMvc.perform(get("/v1/customers/controller-customer-1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value("alice@example.com"))

        val updateBody = """
            { "source_id": "controller-customer-1", "email": "updated@example.com", "name": "Alice Updated" }
        """.trimIndent()
        mockMvc.perform(
            put("/v1/customers/controller-customer-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody)
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value("updated@example.com"))

        mockMvc.perform(delete("/v1/customers/controller-customer-1"))
            .andExpect(status().isNoContent)

        mockMvc.perform(get("/v1/customers/controller-customer-1"))
            .andExpect(status().isNotFound)
    }
}
