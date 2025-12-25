package org.wahlen.voucherengine.api.controller

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.api.tenantJwt
import org.wahlen.voucherengine.config.IntegrationTest
import org.wahlen.voucherengine.persistence.model.tenant.Tenant
import org.wahlen.voucherengine.persistence.repository.TenantRepository
import tools.jackson.databind.ObjectMapper

@IntegrationTest
@AutoConfigureMockMvc
@Transactional
class ProductCollectionControllerIntegrationTest @Autowired constructor(
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
    fun `create update and list product collections`() {
        val productBody = """
            { "name": "Premium Plan", "source_id": "prod-premium", "price": 10000 }
        """.trimIndent()
        val productResult = mockMvc.perform(
            post("/v1/products")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(productBody)
        ).andExpect(status().isOk)
            .andReturn()
        val productId = objectMapper.readTree(productResult.response.contentAsString).get("id").asString()

        val skuBody = """
            { "sku": "Premium Monthly", "source_id": "sku-premium-monthly", "price": 10000, "currency": "USD" }
        """.trimIndent()
        val skuResult = mockMvc.perform(
            post("/v1/products/$productId/skus")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(skuBody)
        ).andExpect(status().isOk)
            .andReturn()
        val skuId = objectMapper.readTree(skuResult.response.contentAsString).get("id").asString()

        val collectionBody = """
            {
              "name": "Featured",
              "type": "STATIC",
              "products": [
                { "id": "$productId", "object": "product" },
                { "id": "$skuId", "product_id": "$productId", "object": "sku" }
              ]
            }
        """.trimIndent()
        val createCollection = mockMvc.perform(
            post("/v1/product-collections")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(collectionBody)
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Featured"))
            .andReturn()
        val collectionId = objectMapper.readTree(createCollection.response.contentAsString).get("id").asString()

        mockMvc.perform(
            get("/v1/product-collections")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(1))

        mockMvc.perform(
            get("/v1/product-collections/$collectionId")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.products[0].object").value("product"))

        mockMvc.perform(
            get("/v1/product-collections/$collectionId/products")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(2))
            .andExpect(jsonPath("$.data[*].object").isArray)

        val updateBody = """
            { "name": "Featured Updated" }
        """.trimIndent()
        mockMvc.perform(
            put("/v1/product-collections/$collectionId")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody)
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Featured Updated"))

        mockMvc.perform(
            delete("/v1/product-collections/$collectionId")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
        ).andExpect(status().isNoContent)
    }

    @Test
    fun `auto update collection evaluates filter`() {
        val productBody = """
            { "name": "EU Plan", "source_id": "prod-eu", "price": 9000, "metadata": { "region": "EU" } }
        """.trimIndent()
        mockMvc.perform(
            post("/v1/products")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(productBody)
        ).andExpect(status().isOk)

        val collectionBody = """
            {
              "name": "EU Collection",
              "type": "AUTO_UPDATE",
              "filter": {
                "junction": "and",
                "metadata.region": {
                  "conditions": { "${'$'}eq": "EU" }
                }
              }
            }
        """.trimIndent()
        val createCollection = mockMvc.perform(
            post("/v1/product-collections")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(collectionBody)
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.type").value("AUTO_UPDATE"))
            .andReturn()
        val collectionId = objectMapper.readTree(createCollection.response.contentAsString).get("id").asString()

        mockMvc.perform(
            get("/v1/product-collections/$collectionId/products")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(1))
    }

    @Test
    fun `auto update collection evaluates sku filter`() {
        val productBody = """
            { "name": "Base Plan", "source_id": "prod-base", "price": 8000, "metadata": { "region": "US" } }
        """.trimIndent()
        val productResult = mockMvc.perform(
            post("/v1/products")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(productBody)
        ).andExpect(status().isOk)
            .andReturn()
        val productId = objectMapper.readTree(productResult.response.contentAsString).get("id").asString()

        val skuBody = """
            { "sku": "Base Gold", "source_id": "sku-base-gold", "price": 8000, "currency": "USD", "metadata": { "tier": "gold" } }
        """.trimIndent()
        mockMvc.perform(
            post("/v1/products/$productId/skus")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(skuBody)
        ).andExpect(status().isOk)

        val collectionBody = """
            {
              "name": "Gold Skus",
              "type": "AUTO_UPDATE",
              "filter": {
                "junction": "and",
                "metadata.tier": {
                  "conditions": { "${'$'}eq": "gold" }
                }
              }
            }
        """.trimIndent()
        val createCollection = mockMvc.perform(
            post("/v1/product-collections")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(collectionBody)
        ).andExpect(status().isOk)
            .andReturn()
        val collectionId = objectMapper.readTree(createCollection.response.contentAsString).get("id").asString()

        mockMvc.perform(
            get("/v1/product-collections/$collectionId/products")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.data[0].object").value("sku"))
    }
}
