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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.api.tenantJwt
import org.wahlen.voucherengine.persistence.model.tenant.Tenant
import org.wahlen.voucherengine.persistence.repository.TenantRepository
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
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
            ).andExpect(status().isCreated)
        }

        // Bulk update metadata
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
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.success_count").value(2))
            .andExpect(jsonPath("$.failure_count").value(0))
            .andExpect(jsonPath("$.failed_codes").isEmpty)

        // Verify metadata was updated
        mockMvc.perform(
            get("/v1/vouchers/$code1")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.metadata.updated").value(true))
            .andExpect(jsonPath("$.metadata.value").value(1))
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
        ).andExpect(status().isCreated)

        // Try to update two, one non-existent
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
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.success_count").value(1))
            .andExpect(jsonPath("$.failure_count").value(1))
            .andExpect(jsonPath("$.failed_codes[0]").value("NON-EXISTENT"))
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
        ).andExpect(status().isCreated)

        // Update only specific metadata fields
        val updateBody = """
            { "codes": ["$code"], "metadata": { "version": 2, "updated_at": "2025-01-01" } }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/vouchers/metadata/async")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody)
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.success_count").value(1))

        // Verify metadata was merged (source still exists, version updated, updated_at added)
        mockMvc.perform(
            get("/v1/vouchers/$code")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.metadata.source").value("test"))
            .andExpect(jsonPath("$.metadata.version").value(2))
            .andExpect(jsonPath("$.metadata.updated_at").value("2025-01-01"))
    }
}
