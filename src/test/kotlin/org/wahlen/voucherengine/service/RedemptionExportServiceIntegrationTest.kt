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
import org.wahlen.voucherengine.persistence.model.customer.Customer
import org.wahlen.voucherengine.persistence.model.redemption.Redemption
import org.wahlen.voucherengine.persistence.model.redemption.RedemptionResult
import org.wahlen.voucherengine.persistence.model.redemption.RedemptionStatus
import org.wahlen.voucherengine.persistence.model.tenant.Tenant
import org.wahlen.voucherengine.persistence.model.voucher.Voucher
import org.wahlen.voucherengine.persistence.model.voucher.VoucherType
import org.wahlen.voucherengine.persistence.repository.AsyncJobRepository
import org.wahlen.voucherengine.persistence.repository.CampaignRepository
import org.wahlen.voucherengine.persistence.repository.CustomerRepository
import org.wahlen.voucherengine.persistence.repository.RedemptionRepository
import org.wahlen.voucherengine.persistence.repository.TenantRepository
import org.wahlen.voucherengine.persistence.repository.VoucherRepository
import org.wahlen.voucherengine.service.async.AsyncJobPublisher
import org.wahlen.voucherengine.service.async.command.RedemptionExportCommand
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import java.time.Duration
import java.util.concurrent.TimeUnit

@S3IntegrationTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RedemptionExportServiceIntegrationTest {

    @Autowired
    private lateinit var redemptionExportService: RedemptionExportService

    @Autowired
    private lateinit var asyncJobPublisher: AsyncJobPublisher

    @Autowired
    private lateinit var asyncJobRepository: AsyncJobRepository

    @Autowired
    private lateinit var redemptionRepository: RedemptionRepository

    @Autowired
    private lateinit var voucherRepository: VoucherRepository

    @Autowired
    private lateinit var campaignRepository: CampaignRepository

    @Autowired
    private lateinit var customerRepository: CustomerRepository

    @Autowired
    private lateinit var tenantRepository: TenantRepository

    @Autowired
    private lateinit var exportRepository: org.wahlen.voucherengine.persistence.repository.ExportRepository

    @Autowired
    private lateinit var s3Client: S3Client

    private lateinit var tenant: Tenant
    private lateinit var campaign: Campaign
    private lateinit var customer: Customer

    @BeforeEach
    fun setup() {
        // Clean up THIS tenant's data only
        val tenantName = "test-redemption-export-tenant"
        
        redemptionRepository.findAllByTenantName(tenantName).forEach { redemptionRepository.delete(it) }
        voucherRepository.findAllByTenantName(tenantName).forEach { voucherRepository.delete(it) }
        campaignRepository.findAllByTenantName(tenantName).forEach { campaignRepository.delete(it) }
        customerRepository.findAllByTenantName(tenantName).forEach { customerRepository.delete(it) }
        asyncJobRepository.findAllByTenant_Name(tenantName, org.springframework.data.domain.Pageable.unpaged()).content.forEach { asyncJobRepository.delete(it) }
        exportRepository.findAllByTenantName(tenantName).forEach { exportRepository.delete(it) }
        tenantRepository.findByName(tenantName)?.let { tenantRepository.delete(it) }

        // Create tenant
        tenant = Tenant(name = tenantName)
        tenant = tenantRepository.save(tenant)

        // Create campaign
        campaign = Campaign(
            name = "Test Campaign",
            tenant = tenant
        )
        campaign = campaignRepository.save(campaign)

        // Create customer
        customer = Customer(
            sourceId = "customer-001",
            name = "John Doe",
            email = "john@example.com",
            tenant = tenant
        )
        customer = customerRepository.save(customer)

        // Create vouchers and redemptions
        repeat(3) { i ->
            val voucher = Voucher(
                code = "VOUCHER-${i + 1}",
                campaign = campaign,
                active = true,
                type = VoucherType.DISCOUNT_VOUCHER,
                tenant = tenant
            )
            voucherRepository.save(voucher)

            // Create successful redemption
            val redemption = Redemption(
                voucher = voucher,
                customer = customer,
                result = RedemptionResult.SUCCESS,
                status = RedemptionStatus.SUCCEEDED,
                trackingId = "track-${i + 1}",
                tenant = tenant
            )
            redemptionRepository.save(redemption)
        }

        // Create one failed redemption
        val failedVoucher = Voucher(
            code = "VOUCHER-FAIL",
            campaign = campaign,
            active = false,
            type = VoucherType.DISCOUNT_VOUCHER,
            tenant = tenant
        )
        voucherRepository.save(failedVoucher)

        val failedRedemption = Redemption(
            voucher = failedVoucher,
            customer = customer,
            result = RedemptionResult.FAILURE,
            status = RedemptionStatus.FAILED,
            reason = "Voucher is not active",
            tenant = tenant
        )
        redemptionRepository.save(failedRedemption)
    }

    @Test
    fun `should export redemptions to CSV format`() {
        // Given - Request with specific fields
        val command = RedemptionExportCommand(
            tenantName = tenant.name!!,
            parameters = mapOf(
                "format" to "CSV",
                "fields" to listOf("id", "voucher_code", "customer_name", "result")
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
        assertThat(completedJob.type).isEqualTo(AsyncJobType.REDEMPTION_EXPORT)
        assertThat(completedJob.progress).isEqualTo(4) // 3 successful + 1 failed
        assertThat(completedJob.total).isEqualTo(4)

        val result = completedJob.result as Map<*, *>
        assertThat(result["format"]).isEqualTo("CSV")
        assertThat(result["recordCount"]).isEqualTo(4)
        assertThat(result["url"]).isNotNull()
        assertThat(result["expiresAt"]).isNotNull()

        // Verify S3 file content
        val url = result["url"] as String
        val key = extractS3KeyFromUrl(url)
        val csvContent = downloadFromS3(key)

        assertThat(csvContent).contains("id,voucher_code,customer_name,result")
        assertThat(csvContent).contains("VOUCHER-1")
        assertThat(csvContent).contains("VOUCHER-FAIL")
        assertThat(csvContent).contains("John Doe")
        assertThat(csvContent).contains("SUCCESS")
        assertThat(csvContent).contains("FAILURE")
    }

    @Test
    fun `should export redemptions to JSON format`() {
        // Given
        val command = RedemptionExportCommand(
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
        assertThat(result["recordCount"]).isEqualTo(4)

        // Verify JSON content
        val url = result["url"] as String
        val key = extractS3KeyFromUrl(url)
        val jsonContent = downloadFromS3(key)

        assertThat(jsonContent).contains("\"voucher_code\"")
        assertThat(jsonContent).contains("\"VOUCHER-1\"")
        assertThat(jsonContent).contains("\"result\"")
    }

    @Test
    fun `should handle empty export gracefully`() {
        // Given - Clean all redemptions
        redemptionRepository.deleteAll()

        val command = RedemptionExportCommand(
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
        assertThat(result["message"]).isEqualTo("No redemptions found matching the criteria")
    }

    private fun extractS3KeyFromUrl(url: String): String {
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
