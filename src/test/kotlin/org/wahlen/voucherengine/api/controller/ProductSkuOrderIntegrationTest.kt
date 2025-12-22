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
import tools.jackson.databind.ObjectMapper

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProductSkuOrderIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val tenantRepository: TenantRepository
) {
    private val objectMapper = ObjectMapper()
    private val tenantName = "test-tenant"

    @BeforeEach
    fun setUp() {
        if (tenantRepository.findByName(tenantName) == null) {
            tenantRepository.save(Tenant(name = tenantName))
        }
    }

    @Test
    fun `create update and delete product with skus`() {
        val productBody = """
            { "name": "Premium Plan", "source_id": "prod-premium", "price": 10000, "attributes": ["tier","cycle"], "metadata": { "segment": "pro" } }
        """.trimIndent()

        val createProduct = mockMvc.perform(
            post("/v1/products")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(productBody)
        ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("Premium Plan"))
            .andReturn()

        val productId = objectMapper.readTree(createProduct.response.contentAsString).get("id").asText()

        mockMvc.perform(get("/v1/products").header("tenant", tenantName).with(tenantJwt(tenantName)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.products[0].source_id").value("prod-premium"))

        val skuBody = """
            { "sku": "Premium Monthly", "source_id": "sku-premium-monthly", "price": 10000, "currency": "USD", "attributes": { "cycle": "monthly" } }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/products/$productId/skus")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(skuBody)
        ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.source_id").value("sku-premium-monthly"))

        mockMvc.perform(
            get("/v1/products/$productId/skus")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(1))

        mockMvc.perform(
            get("/v1/skus/sku-premium-monthly")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.sku").value("Premium Monthly"))

        val updateBody = """
            { "sku": "Premium Monthly Updated", "price": 12000 }
        """.trimIndent()

        mockMvc.perform(
            put("/v1/products/$productId/skus/sku-premium-monthly")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody)
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.sku").value("Premium Monthly Updated"))

        mockMvc.perform(
            delete("/v1/products/$productId")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
        ).andExpect(status().isNoContent)

        mockMvc.perform(
            get("/v1/skus/sku-premium-monthly")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
        ).andExpect(status().isNotFound)
    }

    @Test
    fun `order items reference products and skus by id`() {
        val productBody = """
            { "name": "Starter", "source_id": "prod-starter", "price": 5000 }
        """.trimIndent()
        mockMvc.perform(
            post("/v1/products")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(productBody)
        ).andExpect(status().isCreated)

        val skuBody = """
            { "sku": "Starter Monthly", "source_id": "sku-starter-monthly", "price": 5000, "currency": "USD" }
        """.trimIndent()
        val productId = objectMapper.readTree(
            mockMvc.perform(
                get("/v1/products/prod-starter")
                    .header("tenant", tenantName)
                    .with(tenantJwt(tenantName))
            ).andReturn().response.contentAsString
        ).get("id").asText()

        mockMvc.perform(
            post("/v1/products/$productId/skus")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(skuBody)
        ).andExpect(status().isCreated)

        val orderBody = """
            {
              "source_id": "order-products-1",
              "status": "PAID",
              "amount": 5000,
              "items": [
                { "product_id": "prod-starter", "sku_id": "sku-starter-monthly", "quantity": 1, "price": 5000 }
              ],
              "customer": { "source_id": "cust-products", "email": "products@example.com" }
            }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/orders")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderBody)
        ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.items[0].product_id").value("prod-starter"))
            .andExpect(jsonPath("$.items[0].sku_id").value("sku-starter-monthly"))
            .andExpect(jsonPath("$.items[0].amount").value(5000))
            .andExpect(jsonPath("$.items[0].subtotal_amount").value(5000))
            .andExpect(jsonPath("$.items[0].object").value("order_item"))

        mockMvc.perform(
            delete("/v1/products/prod-starter")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
        ).andExpect(status().isNoContent)

        mockMvc.perform(
            get("/v1/orders/order-products-1")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].product_id").value("prod-starter"))
            .andExpect(jsonPath("$.items[0].sku_id").value("sku-starter-monthly"))
    }

    @Test
    fun `lists products with pagination`() {
        val firstProduct = """{ "name": "First", "source_id": "prod-first", "price": 1000 }"""
        val secondProduct = """{ "name": "Second", "source_id": "prod-second", "price": 2000 }"""
        mockMvc.perform(
            post("/v1/products")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(firstProduct)
        ).andExpect(status().isCreated)
        mockMvc.perform(
            post("/v1/products")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(secondProduct)
        ).andExpect(status().isCreated)

        mockMvc.perform(
            get("/v1/products?limit=1&page=1&order=created_at")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(2))
            .andExpect(jsonPath("$.products.length()").value(1))
    }
}
