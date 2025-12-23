package org.wahlen.voucherengine.api.controller

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
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
class CategoryControllerIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val tenantRepository: TenantRepository
) {
    private val tenantName = "test-tenant"
    private val objectMapper = ObjectMapper()

    @BeforeEach
    fun setUp() {
        if (tenantRepository.findByName(tenantName) == null) {
            tenantRepository.save(Tenant(name = tenantName))
        }
    }

    @Test
    fun `category CRUD endpoints`() {
        val createResult = mockMvc.perform(
            post("/v1/categories")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"electronics"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("electronics"))
            .andReturn()

        val created = objectMapper.readTree(createResult.response.contentAsString)
        val id = created.get("id").asString()

        mockMvc.perform(get("/v1/categories/$id").header("tenant", tenantName).with(tenantJwt(tenantName)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("electronics"))

        mockMvc.perform(get("/v1/categories").header("tenant", tenantName).with(tenantJwt(tenantName)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].name").value("electronics"))

        mockMvc.perform(
            put("/v1/categories/$id")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"updated"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("updated"))

        mockMvc.perform(delete("/v1/categories/$id").header("tenant", tenantName).with(tenantJwt(tenantName)))
            .andExpect(status().isNoContent)

        mockMvc.perform(get("/v1/categories/$id").header("tenant", tenantName).with(tenantJwt(tenantName)))
            .andExpect(status().isNotFound)
    }
}
