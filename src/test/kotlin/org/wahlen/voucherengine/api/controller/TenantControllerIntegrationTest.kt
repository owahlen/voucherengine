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
import tools.jackson.databind.ObjectMapper

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TenantControllerIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc
) {
    private val tenantHeader = "bootstrap"
    private val objectMapper = ObjectMapper()

    @Test
    fun `tenant CRUD endpoints`() {
        val createBody = """{ "name": "acme" }"""
        val createResult = mockMvc.perform(
            post("/v1/tenants")
                .header("tenant", tenantHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody)
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("acme"))
            .andReturn()

        val tenantId = objectMapper.readTree(createResult.response.contentAsString).get("id").asText()

        mockMvc.perform(get("/v1/tenants/$tenantId").header("tenant", tenantHeader))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(tenantId))

        mockMvc.perform(get("/v1/tenants").header("tenant", tenantHeader))
            .andExpect(status().isOk)

        val updateBody = """{ "name": "acme-updated" }"""
        mockMvc.perform(
            put("/v1/tenants/$tenantId")
                .header("tenant", tenantHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("acme-updated"))

        mockMvc.perform(delete("/v1/tenants/$tenantId").header("tenant", tenantHeader))
            .andExpect(status().isNoContent)

        mockMvc.perform(get("/v1/tenants/$tenantId").header("tenant", tenantHeader))
            .andExpect(status().isNotFound)
    }
}
