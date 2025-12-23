package org.wahlen.voucherengine.api.controller

import org.junit.jupiter.api.BeforeEach
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
class VoucherImportExportTest @Autowired constructor(
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
    fun `import vouchers creates multiple vouchers`() {
        val code1 = "IMPORT-1-${UUID.randomUUID().toString().take(6)}"
        val code2 = "IMPORT-2-${UUID.randomUUID().toString().take(6)}"

        val importBody = """
            {
                "vouchers": [
                    { 
                        "code": "$code1",
                        "type": "DISCOUNT_VOUCHER",
                        "discount": { "type": "PERCENT", "percent_off": 10 },
                        "metadata": { "source": "import" }
                    },
                    {
                        "code": "$code2",
                        "type": "GIFT_VOUCHER",
                        "gift": { "amount": 5000 }
                    }
                ]
            }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/vouchers/import")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(importBody)
        ).andExpect(status().isAccepted)
            .andExpect(jsonPath("$.async_action_id").exists())
            .andExpect(jsonPath("$.status").value("ACCEPTED"))

        // TODO: Verify vouchers created via /async-actions/{id} once async job processing is implemented
    }

    @Test
    fun `import reports failures for invalid vouchers`() {
        val validCode = "VALID-${UUID.randomUUID().toString().take(6)}"

        val importBody = """
            {
                "vouchers": [
                    { 
                        "code": "$validCode",
                        "type": "DISCOUNT_VOUCHER",
                        "discount": { "type": "PERCENT", "percent_off": 10 }
                    },
                    {
                        "code": "",
                        "type": "INVALID_TYPE"
                    }
                ]
            }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/vouchers/import")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(importBody)
        ).andExpect(status().isAccepted)
            .andExpect(jsonPath("$.async_action_id").exists())
            .andExpect(jsonPath("$.status").value("ACCEPTED"))

        // TODO: Verify failure count via /async-actions/{id} once async job processing is implemented
    }

    @Test
    fun `CSV import returns not implemented`() {
        mockMvc.perform(
            post("/v1/vouchers/importCSV")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
        ).andExpect(status().isNotImplemented)
            .andExpect(jsonPath("$.message").exists())
    }

    @Test
    fun `transaction export returns not implemented`() {
        mockMvc.perform(
            post("/v1/vouchers/TEST-CODE/transactions/export")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
        ).andExpect(status().isNotImplemented)
            .andExpect(jsonPath("$.message").exists())
    }
}
