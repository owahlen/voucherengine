package org.wahlen.voucherengine.api.controller

import org.junit.jupiter.api.BeforeEach
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
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
class ValidationRuleControllerIntegrationTest @Autowired constructor(
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
    fun `create and assign validation rule via controller`() {
        val createBody = """
            { "name": "One redemption per customer", "type": "redemptions", "conditions": { "redemptions": { "per_customer": 1 } } }
        """.trimIndent()

        val createResult = mockMvc.perform(
            post("/v1/validation-rules")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody)
        ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.object").value("validation_rule"))
            .andExpect(jsonPath("$.conditions.redemptions.per_customer").value(1))
            .andReturn()

        val createdId = objectMapper.readTree(createResult.response.contentAsString).get("id").asText()

        val assignBody = """
            { "object": "voucher", "id": "TEST" }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/validation-rules/$createdId/assignments")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(assignBody)
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.object").value("validation_rules_assignment"))
            .andExpect(jsonPath("$.related_object_id").value("TEST"))
            .andExpect(jsonPath("$.rule_id").value(createdId))

        val updateBody = """
            { "name": "Updated Rule", "type": "redemptions", "conditions": { "redemptions": { "per_customer": 2 } } }
        """.trimIndent()

        mockMvc.perform(
            put("/v1/validation-rules/$createdId")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody)
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Updated Rule"))

        // get rule
        mockMvc.perform(get("/v1/validation-rules/$createdId").header("tenant", tenantName).with(tenantJwt(tenantName)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(createdId))

        // list rules
        mockMvc.perform(get("/v1/validation-rules").header("tenant", tenantName).with(tenantJwt(tenantName)))
            .andExpect(status().isOk)

        mockMvc.perform(get("/v1/validation-rules-assignments").header("tenant", tenantName).with(tenantJwt(tenantName)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].rule_id").value(createdId))

        mockMvc.perform(get("/v1/validation-rules/$createdId/assignments").header("tenant", tenantName).with(tenantJwt(tenantName)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].related_object_id").value("TEST"))

        val assignmentId = objectMapper.readTree(
            mockMvc.perform(get("/v1/validation-rules-assignments").header("tenant", tenantName).with(tenantJwt(tenantName)))
                .andReturn().response.contentAsString
        ).first().get("id").asText()

        mockMvc.perform(delete("/v1/validation-rules-assignments/$assignmentId").header("tenant", tenantName).with(tenantJwt(tenantName)))
            .andExpect(status().isNoContent)

        // delete rule
        mockMvc.perform(delete("/v1/validation-rules/$createdId").header("tenant", tenantName).with(tenantJwt(tenantName)))
            .andExpect(status().isNoContent)

        mockMvc.perform(get("/v1/validation-rules/$createdId").header("tenant", tenantName).with(tenantJwt(tenantName)))
            .andExpect(status().isNotFound)
    }
}
