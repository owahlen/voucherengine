package org.wahlen.voucherengine.api.controller

import org.awaitility.kotlin.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.wahlen.voucherengine.api.tenantJwt
import org.wahlen.voucherengine.config.SqsIntegrationTest
import org.wahlen.voucherengine.persistence.model.async.AsyncJobStatus
import org.wahlen.voucherengine.persistence.model.tenant.Tenant
import org.wahlen.voucherengine.persistence.repository.AsyncJobRepository
import org.wahlen.voucherengine.persistence.repository.TenantRepository
import org.wahlen.voucherengine.persistence.repository.VoucherRepository
import tools.jackson.databind.ObjectMapper
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@SqsIntegrationTest
@AutoConfigureMockMvc
class VoucherAsyncIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val tenantRepository: TenantRepository,
    private val asyncJobRepository: AsyncJobRepository,
    private val voucherRepository: VoucherRepository,
    private val objectMapper: ObjectMapper
) {
    private val tenantName = "test-tenant"

    @BeforeEach
    fun setUp() {
        if (tenantRepository.findByName(tenantName) == null) {
            tenantRepository.save(Tenant(name = tenantName))
        }
    }

    @org.junit.jupiter.api.AfterEach
    fun tearDown() {
        // Clean up test data
        asyncJobRepository.deleteAll()
        voucherRepository.deleteAll()
    }

    @Test
    fun `bulk update vouchers creates async job and processes it`() {
        // Create test vouchers
        val code1 = "BULK-${UUID.randomUUID().toString().take(6)}"
        val code2 = "BULK-${UUID.randomUUID().toString().take(6)}"

        val createBody1 = """
            { "code": "$code1", "type": "DISCOUNT_VOUCHER", "discount": { "type": "PERCENT", "percent_off": 10 } }
        """.trimIndent()
        val createBody2 = """
            { "code": "$code2", "type": "DISCOUNT_VOUCHER", "discount": { "type": "PERCENT", "percent_off": 10 } }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/vouchers")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody1)
        ).andExpect(status().isOk)

        mockMvc.perform(
            post("/v1/vouchers")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody2)
        ).andExpect(status().isOk)

        // Submit bulk update
        val bulkUpdateBody = """
            [
                {
                    "code": "$code1",
                    "metadata": { "updated": "true", "batch": "1" }
                },
                {
                    "code": "$code2",
                    "metadata": { "updated": "true", "batch": "1" }
                }
            ]
        """.trimIndent()

        val result = mockMvc.perform(
            post("/v1/vouchers/bulk/async")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(bulkUpdateBody)
        ).andExpect(status().isAccepted)
            .andExpect(jsonPath("$.async_action_id").exists())
            .andExpect(jsonPath("$.status").value("ACCEPTED"))
            .andReturn()

        val response = objectMapper.readValue(result.response.contentAsString, Map::class.java)
        val jobId = UUID.fromString(response["async_action_id"] as String)

        // Wait for job to complete using Awaitility
        await.atMost(10, TimeUnit.SECONDS).untilAsserted {
            val job = asyncJobRepository.findById(jobId).orElse(null)
            assertNotNull(job, "Job should exist")
            assertEquals(AsyncJobStatus.COMPLETED, job.status, "Job should be completed")
        }

        // Verify job status via API
        mockMvc.perform(
            get("/v1/async-actions/$jobId")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(jobId.toString()))
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.progress").value(2))
            .andExpect(jsonPath("$.total").value(2))

        // Verify vouchers were actually updated
        val voucher1 = voucherRepository.findByCodeAndTenantName(code1, tenantName)
        val voucher2 = voucherRepository.findByCodeAndTenantName(code2, tenantName)

        assertNotNull(voucher1)
        assertNotNull(voucher2)
        assertEquals("true", (voucher1.metadata as? Map<*, *>)?.get("updated"))
        assertEquals("1", (voucher1.metadata as? Map<*, *>)?.get("batch"))
        assertEquals("true", (voucher2.metadata as? Map<*, *>)?.get("updated"))
        assertEquals("1", (voucher2.metadata as? Map<*, *>)?.get("batch"))
    }

    @Test
    fun `metadata update creates async job and processes it`() {
        val code = "META-${UUID.randomUUID().toString().take(6)}"

        val createBody = """
            { "code": "$code", "type": "DISCOUNT_VOUCHER", "discount": { "type": "PERCENT", "percent_off": 15 }, "metadata": { "original": "true" } }
        """.trimIndent()

        mockMvc.perform(
            post("/v1/vouchers")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody)
        ).andExpect(status().isOk)

        // Submit metadata update
        val metadataUpdateBody = """
            {
                "codes": ["$code"],
                "metadata": { "updated": "true", "newField": "newValue" }
            }
        """.trimIndent()

        val result = mockMvc.perform(
            post("/v1/vouchers/metadata/async")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(metadataUpdateBody)
        ).andExpect(status().isAccepted)
            .andExpect(jsonPath("$.async_action_id").exists())
            .andExpect(jsonPath("$.status").value("ACCEPTED"))
            .andReturn()

        val response = objectMapper.readValue(result.response.contentAsString, Map::class.java)
        val jobId = UUID.fromString(response["async_action_id"] as String)

        // Wait for job completion
        await.atMost(10, TimeUnit.SECONDS).untilAsserted {
            val job = asyncJobRepository.findById(jobId).orElse(null)
            assertNotNull(job, "Job should exist")
            assertEquals(AsyncJobStatus.COMPLETED, job.status, "Job should be completed")
        }

        // Verify metadata was updated
        val voucher = voucherRepository.findByCodeAndTenantName(code, tenantName)

        assertNotNull(voucher)
        assertEquals("true", (voucher.metadata as? Map<*, *>)?.get("updated"))
        assertEquals("newValue", (voucher.metadata as? Map<*, *>)?.get("newField"))
        // Original metadata should be preserved
        assertEquals("true", (voucher.metadata as? Map<*, *>)?.get("original"))
    }

    @Test
    fun `voucher import creates async job and processes it`() {
        val code1 = "IMP-${UUID.randomUUID().toString().take(6)}"
        val code2 = "IMP-${UUID.randomUUID().toString().take(6)}"

        // Submit voucher import
        val importBody = """
            {
                "vouchers": [
                    {
                        "code": "$code1",
                        "type": "DISCOUNT_VOUCHER",
                        "discount": { "type": "AMOUNT", "amount_off": 500 },
                        "metadata": { "source": "import" }
                    },
                    {
                        "code": "$code2",
                        "type": "GIFT_VOUCHER",
                        "gift": { "amount": 10000 },
                        "metadata": { "source": "import" }
                    }
                ]
            }
        """.trimIndent()

        val result = mockMvc.perform(
            post("/v1/vouchers/import")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(importBody)
        ).andExpect(status().isAccepted)
            .andExpect(jsonPath("$.async_action_id").exists())
            .andExpect(jsonPath("$.status").value("ACCEPTED"))
            .andReturn()

        val response = objectMapper.readValue(result.response.contentAsString, Map::class.java)
        val jobId = UUID.fromString(response["async_action_id"] as String)

        // Wait for job completion
        await.atMost(10, TimeUnit.SECONDS).untilAsserted {
            val job = asyncJobRepository.findById(jobId).orElse(null)
            assertNotNull(job, "Job should exist")
            assertEquals(AsyncJobStatus.COMPLETED, job.status, "Job should be completed")
        }

        // Verify job status
        mockMvc.perform(
            get("/v1/async-actions/$jobId")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.progress").value(2))
            .andExpect(jsonPath("$.total").value(2))

        // Verify vouchers were created
        val voucher1 = voucherRepository.findByCodeAndTenantName(code1, tenantName)
        val voucher2 = voucherRepository.findByCodeAndTenantName(code2, tenantName)

        assertNotNull(voucher1)
        assertNotNull(voucher2)
        assertEquals("import", (voucher1.metadata as? Map<*, *>)?.get("source"))
        assertEquals("import", (voucher2.metadata as? Map<*, *>)?.get("source"))
    }

    @Test
    fun `async job not found returns 404`() {
        val nonExistentId = UUID.randomUUID()

        mockMvc.perform(
            get("/v1/async-actions/$nonExistentId")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
        ).andExpect(status().isNotFound)
    }

    @Test
    fun `bulk update with invalid voucher marks job as failed`() {
        val nonExistentCode = "NONEXISTENT-${UUID.randomUUID()}"

        val bulkUpdateBody = """
            [
                {
                    "code": "$nonExistentCode",
                    "metadata": { "updated": "true" }
                }
            ]
        """.trimIndent()

        val result = mockMvc.perform(
            post("/v1/vouchers/bulk/async")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(bulkUpdateBody)
        ).andExpect(status().isAccepted)
            .andReturn()

        val response = objectMapper.readValue(result.response.contentAsString, Map::class.java)
        val jobId = UUID.fromString(response["async_action_id"] as String)

        // Wait for job to complete (should complete with failures tracked)
        await.atMost(10, TimeUnit.SECONDS).untilAsserted {
            val job = asyncJobRepository.findById(jobId).orElse(null)
            assertNotNull(job, "Job should exist")
            assertEquals(AsyncJobStatus.COMPLETED, job.status, "Job should complete even with invalid vouchers")
        }

        // Verify failed codes are tracked in result
        mockMvc.perform(
            get("/v1/async-actions/$jobId")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.result.failure_count").value(1))
            .andExpect(jsonPath("$.result.failed_codes[0]").value(nonExistentCode))
    }
}
