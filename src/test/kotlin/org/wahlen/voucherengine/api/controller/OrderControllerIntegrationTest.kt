package org.wahlen.voucherengine.api.controller

import org.junit.jupiter.api.BeforeEach
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
import org.wahlen.voucherengine.api.tenantJwt
import org.wahlen.voucherengine.persistence.model.tenant.Tenant
import org.wahlen.voucherengine.persistence.repository.TenantRepository

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrderControllerIntegrationTest @Autowired constructor(
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
    fun `create update and delete order`() {
        val createBody = """
            {
              "source_id": "order-1",
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
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody)
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.source_id").value("order-1"))
            .andExpect(jsonPath("$.customer_id").exists())
            .andExpect(jsonPath("$.customer.object").value("customer"))

        mockMvc.perform(get("/v1/orders").header("tenant", tenantName).with(tenantJwt(tenantName)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.orders[0].source_id").value("order-1"))

        mockMvc.perform(get("/v1/orders/order-1").header("tenant", tenantName).with(tenantJwt(tenantName)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("PAID"))

        val updateBody = """
            {
              "source_id": "order-1",
              "status": "CANCELED",
              "discount_amount": 300
            }
        """.trimIndent()

        mockMvc.perform(
            put("/v1/orders/order-1")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("CANCELED"))
            .andExpect(jsonPath("$.discount_amount").value(300))

        mockMvc.perform(delete("/v1/orders/order-1").header("tenant", tenantName).with(tenantJwt(tenantName)))
            .andExpect(status().isNoContent)

        mockMvc.perform(get("/v1/orders/order-1").header("tenant", tenantName).with(tenantJwt(tenantName)))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `rejects invalid order request`() {
        mockMvc.perform(
            post("/v1/orders")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `lists orders with pagination`() {
        val firstOrder = """
            { "source_id": "order-a", "status": "PAID", "amount": 500 }
        """.trimIndent()
        val secondOrder = """
            { "source_id": "order-b", "status": "PAID", "amount": 700 }
        """.trimIndent()
        mockMvc.perform(
            post("/v1/orders")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(firstOrder)
        ).andExpect(status().isCreated)
        mockMvc.perform(
            post("/v1/orders")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(secondOrder)
        ).andExpect(status().isCreated)

        mockMvc.perform(
            get("/v1/orders?limit=1&page=1&order=created_at")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(2))
            .andExpect(jsonPath("$.orders.length()").value(1))
    }
}
