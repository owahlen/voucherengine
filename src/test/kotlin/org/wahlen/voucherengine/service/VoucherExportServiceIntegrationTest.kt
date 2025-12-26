package org.wahlen.voucherengine.service

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.annotation.DirtiesContext
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.config.S3IntegrationTest
import org.wahlen.voucherengine.persistence.model.async.AsyncJobStatus
import org.wahlen.voucherengine.persistence.model.async.AsyncJobType
import org.wahlen.voucherengine.persistence.model.campaign.Campaign
import org.wahlen.voucherengine.persistence.model.tenant.Tenant
import org.wahlen.voucherengine.persistence.model.voucher.Voucher
import org.wahlen.voucherengine.persistence.repository.AsyncJobRepository
import org.wahlen.voucherengine.persistence.repository.CampaignRepository
import org.wahlen.voucherengine.persistence.repository.TenantRepository
import org.wahlen.voucherengine.persistence.repository.VoucherRepository
import org.wahlen.voucherengine.service.async.AsyncJobPublisher
import org.wahlen.voucherengine.service.async.command.VoucherExportCommand
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import java.time.Duration
import java.util.concurrent.TimeUnit

@S3IntegrationTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class VoucherExportServiceIntegrationTest {

    @Autowired
    private lateinit var voucherExportService: VoucherExportService

    @Autowired
    private lateinit var asyncJobPublisher: AsyncJobPublisher

    @Autowired
    private lateinit var asyncJobRepository: AsyncJobRepository

    @Autowired
    private lateinit var voucherRepository: VoucherRepository

    @Autowired
    private lateinit var campaignRepository: CampaignRepository

    @Autowired
    private lateinit var tenantRepository: TenantRepository

    @Autowired
    private lateinit var exportRepository: org.wahlen.voucherengine.persistence.repository.ExportRepository

    @Autowired
    private lateinit var s3Client: S3Client

    private lateinit var tenant: Tenant
    private lateinit var campaign: Campaign

    @BeforeEach
    fun setup() {
        // Clean up THIS tenant's data only (don't use deleteAll() - it affects other tests!)
        val tenantName = "test-voucher-export-tenant"
        
        // Find and delete only this tenant's entities  
        voucherRepository.findAllByTenantName(tenantName).forEach { voucherRepository.delete(it) }
        campaignRepository.findAllByTenantName(tenantName).forEach { campaignRepository.delete(it) }
        asyncJobRepository.findAllByTenant_Name(tenantName, org.springframework.data.domain.Pageable.unpaged()).content.forEach { asyncJobRepository.delete(it) }
        exportRepository.findAllByTenantName(tenantName).forEach { exportRepository.delete(it) }
        tenantRepository.findByName(tenantName)?.let { tenantRepository.delete(it) }

        // Create tenant
        tenant = Tenant(name = tenantName)
        tenant = tenantRepository.save(tenant)

        // Create campaign
        campaign = Campaign(
            name = "Summer Sale",
            tenant = tenant
        )
        campaign = campaignRepository.save(campaign)

        // Create test vouchers
        repeat(5) { i ->
            val voucher = Voucher(
                code = "VOUCHER-${i + 1}",
                campaign = campaign,
                active = true,
                tenant = tenant
            )
            voucherRepository.save(voucher)
        }
    }

    @Test
    fun `should export vouchers to CSV format`() {
        // Given - Request with specific fields
        val command = VoucherExportCommand(
            tenantName = tenant.name!!,
            parameters = mapOf(
                "format" to "CSV",
                "fields" to listOf("id", "code", "campaign", "active")
            )
        )

        // When
        val jobId = asyncJobPublisher.publish(command, tenant)

        // Then - Wait for async job to complete
        await()
            .atMost(30, TimeUnit.SECONDS)
            .pollInterval(Duration.ofMillis(500))
            .untilAsserted {
                val job = asyncJobRepository.findById(jobId).orElseThrow()
                assertThat(job.status).isIn(AsyncJobStatus.COMPLETED, AsyncJobStatus.FAILED)
                
                if (job.status == AsyncJobStatus.FAILED) {
                    throw AssertionError("Job failed: ${job.result}")
                }
            }

        // Verify job result
        val completedJob = asyncJobRepository.findById(jobId).orElseThrow()
        assertThat(completedJob.status).isEqualTo(AsyncJobStatus.COMPLETED)
        assertThat(completedJob.type).isEqualTo(AsyncJobType.VOUCHER_EXPORT)
        assertThat(completedJob.progress).isEqualTo(5)
        assertThat(completedJob.total).isEqualTo(5)

        val result = completedJob.result as Map<*, *>
        assertThat(result["format"]).isEqualTo("CSV")
        assertThat(result["recordCount"]).isEqualTo(5)
        assertThat(result["url"]).isNotNull()
        assertThat(result["expiresAt"]).isNotNull()

        // Verify S3 file content
        val url = result["url"] as String
        val key = extractS3KeyFromUrl(url)
        val csvContent = downloadFromS3(key)

        assertThat(csvContent).contains("id,code,campaign,active")
        assertThat(csvContent).contains("VOUCHER-1")
        assertThat(csvContent).contains("VOUCHER-5")
        assertThat(csvContent).contains("Summer Sale")
    }

    @Test
    fun `should export vouchers to JSON format`() {
        // Given
        val command = VoucherExportCommand(
            tenantName = tenant.name!!,
            parameters = mapOf("format" to "JSON")
        )

        // When
        val jobId = asyncJobPublisher.publish(command, tenant)

        // Then
        await()
            .atMost(30, TimeUnit.SECONDS)
            .pollInterval(Duration.ofMillis(500))
            .untilAsserted {
                val job = asyncJobRepository.findById(jobId).orElseThrow()
                assertThat(job.status).isIn(AsyncJobStatus.COMPLETED, AsyncJobStatus.FAILED)
                
                if (job.status == AsyncJobStatus.FAILED) {
                    throw AssertionError("Job failed: ${job.result}")
                }
            }

        val completedJob = asyncJobRepository.findById(jobId).orElseThrow()
        assertThat(completedJob.status).isEqualTo(AsyncJobStatus.COMPLETED)

        val result = completedJob.result as Map<*, *>
        assertThat(result["format"]).isEqualTo("JSON")
        assertThat(result["recordCount"]).isEqualTo(5)

        // Verify JSON content
        val url = result["url"] as String
        val key = extractS3KeyFromUrl(url)
        val jsonContent = downloadFromS3(key)

        assertThat(jsonContent).contains("\"code\"")
        assertThat(jsonContent).contains("\"VOUCHER-1\"")
    }

    @Test
    fun `should handle empty export gracefully`() {
        // Given - Clean all vouchers
        voucherRepository.deleteAll()

        val command = VoucherExportCommand(
            tenantName = tenant.name!!,
            parameters = mapOf("format" to "CSV")
        )

        // When
        val jobId = asyncJobPublisher.publish(command, tenant)

        // Then
        await()
            .atMost(30, TimeUnit.SECONDS)
            .pollInterval(Duration.ofMillis(500))
            .untilAsserted {
                val job = asyncJobRepository.findById(jobId).orElseThrow()
                assertThat(job.status).isEqualTo(AsyncJobStatus.COMPLETED)
            }

        val completedJob = asyncJobRepository.findById(jobId).orElseThrow()
        assertThat(completedJob.progress).isEqualTo(0)
        assertThat(completedJob.total).isEqualTo(0)

        val result = completedJob.result as Map<*, *>
        assertThat(result["recordCount"]).isEqualTo(0)
        assertThat(result["message"]).isEqualTo("No vouchers found matching the criteria")
    }

    private fun extractS3KeyFromUrl(url: String): String {
        // URL format: http://localhost:xxxxx/voucherengine-exports/tenant/exports/2024/12/file.csv?...
        val parts = url.split("/")
        val bucketIndex = parts.indexOfFirst { it == "voucherengine-exports" }
        return parts.drop(bucketIndex + 1).joinToString("/").substringBefore("?")
    }

    private fun downloadFromS3(key: String): String {
        val getRequest = GetObjectRequest.builder()
            .bucket("voucherengine-exports")
            .key(key)
            .build()

        return s3Client.getObject(getRequest).use { response ->
            response.readAllBytes().toString(Charsets.UTF_8)
        }
    }
}
