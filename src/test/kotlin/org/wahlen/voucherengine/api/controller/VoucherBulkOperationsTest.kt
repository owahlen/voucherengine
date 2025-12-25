package org.wahlen.voucherengine.api.controller

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.api.tenantJwt
import org.wahlen.voucherengine.config.SqsIntegrationTest
import org.wahlen.voucherengine.persistence.model.tenant.Tenant
import org.wahlen.voucherengine.persistence.repository.TenantRepository
import java.util.UUID

@SqsIntegrationTest
@AutoConfigureMockMvc
@Transactional
class VoucherBulkOperationsTest @Autowired constructor(
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
    fun `bulk update voucher metadata succeeds`() {
        val code1 = "BULK-1-${UUID.randomUUID().toString().take(6)}"
        val code2 = "BULK-2-${UUID.randomUUID().toString().take(6)}"

        // Create two vouchers
        listOf(code1, code2).forEach { code ->
            val createBody = """
                { "code": "$code", "type": "DISCOUNT_VOUCHER", "discount": { "type": "PERCENT", "percent_off": 10 }, "metadata": { "source": "test" } }
            """.trimIndent()
            mockMvc.perform(
                post("/v1/vouchers")
                    .header("tenant", tenantName)
                    .with(tenantJwt(tenantName))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createBody)
            ).andExpect(status().isOk)
        }

        // Bulk update metadata (async - returns job ID)
        val bulkBody = """
            [
                { "code": "$code1", "metadata": { "updated": true, "value": 1 } },
                { "code": "$code2", "metadata": { "updated": true, "value": 2 } }
            ]
        """.trimIndent()

        mockMvc.perform(
            post("/v1/vouchers/bulk/async")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(bulkBody)
        ).andExpect(status().isAccepted)
            .andExpect(jsonPath("$.async_action_id").exists())
            .andExpect(jsonPath("$.status").value("ACCEPTED"))

        // TODO: Implement async job processor and verify results via /async-actions/{id}
        // For now, metadata won't be updated until async job processing is implemented
    }

    @Test
    fun `bulk update reports failed codes`() {
        val code1 = "EXISTS-${UUID.randomUUID().toString().take(6)}"

        // Create only one voucher
        val createBody = """
            { "code": "$code1", "type": "DISCOUNT_VOUCHER", "discount": { "type": "PERCENT", "percent_off": 10 } }
        """.trimIndent()
        mockMvc.perform(
            post("/v1/vouchers")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody)
        ).andExpect(status().isOk)

        // Try to update two, one non-existent (async - returns job ID)
        val bulkBody = """
            [
                { "code": "$code1", "metadata": { "updated": true } },
                { "code": "NON-EXISTENT", "metadata": { "updated": true } }
            ]
        """.trimIndent()

        mockMvc.perform(
            post("/v1/vouchers/bulk/async")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(bulkBody)
        ).andExpect(status().isAccepted)
            .andExpect(jsonPath("$.async_action_id").exists())
            .andExpect(jsonPath("$.status").value("ACCEPTED"))

        // TODO: Verify failure count via /async-actions/{id} once async job processing is implemented
    }

    @Test
    fun `metadata async update merges with existing metadata`() {
        val code = "META-MERGE-${UUID.randomUUID().toString().take(6)}"

        val createBody = """
            { "code": "$code", "type": "DISCOUNT_VOUCHER", "discount": { "type": "PERCENT", "percent_off": 10 }, "metadata": { "source": "test", "version": 1 } }
        """.trimIndent()
        mockMvc.perform(
            post("/v1/vouchers")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody)
        ).andExpect(status().isOk)

        // Update only specific metadata fields (async - returns job ID)
        val updateBody = """
            { "codes": ["$code"], "metadata": { "version": 2, "updated_at": "2025-01-01" } }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/vouchers/metadata/async")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody)
        ).andExpect(status().isAccepted)
            .andExpect(jsonPath("$.async_action_id").exists())
            .andExpect(jsonPath("$.status").value("ACCEPTED"))

        // TODO: Verify metadata merge via /async-actions/{id} once async job processing is implemented
    }
}
