package org.wahlen.voucherengine.service

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.annotation.DirtiesContext
import org.wahlen.voucherengine.config.S3IntegrationTest
import org.wahlen.voucherengine.persistence.model.async.AsyncJobStatus
import org.wahlen.voucherengine.persistence.model.async.AsyncJobType
import org.wahlen.voucherengine.persistence.model.campaign.Campaign
import org.wahlen.voucherengine.persistence.model.customer.Customer
import org.wahlen.voucherengine.persistence.model.publication.Publication
import org.wahlen.voucherengine.persistence.model.publication.PublicationResult
import org.wahlen.voucherengine.persistence.model.tenant.Tenant
import org.wahlen.voucherengine.persistence.model.voucher.Voucher
import org.wahlen.voucherengine.persistence.model.voucher.VoucherType
import org.wahlen.voucherengine.persistence.repository.AsyncJobRepository
import org.wahlen.voucherengine.persistence.repository.CampaignRepository
import org.wahlen.voucherengine.persistence.repository.CustomerRepository
import org.wahlen.voucherengine.persistence.repository.PublicationRepository
import org.wahlen.voucherengine.persistence.repository.TenantRepository
import org.wahlen.voucherengine.persistence.repository.VoucherRepository
import org.wahlen.voucherengine.service.async.AsyncJobPublisher
import org.wahlen.voucherengine.service.async.command.PublicationExportCommand
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import java.time.Duration
import java.util.concurrent.TimeUnit

@S3IntegrationTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PublicationExportServiceIntegrationTest {

    @Autowired
    private lateinit var publicationExportService: PublicationExportService

    @Autowired
    private lateinit var asyncJobPublisher: AsyncJobPublisher

    @Autowired
    private lateinit var asyncJobRepository: AsyncJobRepository

    @Autowired
    private lateinit var publicationRepository: PublicationRepository

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
        val tenantName = "test-publication-export-tenant"
        
        publicationRepository.findAllByTenantName(tenantName).forEach { publicationRepository.delete(it) }
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
            name = "Jane Doe",
            email = "jane@example.com",
            tenant = tenant
        )
        customer = customerRepository.save(customer)

        // Create vouchers and publications
        repeat(3) { i ->
            val voucher = Voucher(
                code = "PUBVOUCHER-${i + 1}",
                campaign = campaign,
                active = true,
                type = VoucherType.DISCOUNT_VOUCHER,
                tenant = tenant
            )
            voucherRepository.save(voucher)

            // Create successful publication
            val publication = Publication(
                voucher = voucher,
                customer = customer,
                campaign = campaign,
                channel = "API",
                result = PublicationResult.SUCCESS,
                tenant = tenant
            )
            publicationRepository.save(publication)
        }
    }

    @Test
    fun `should export publications to CSV format`() {
        // Given - Request with specific fields
        val command = PublicationExportCommand(
            tenantName = tenant.name!!,
            parameters = mapOf(
                "format" to "CSV",
                "fields" to listOf("code", "customer_id", "campaign", "channel", "date")
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
        assertThat(completedJob.type).isEqualTo(AsyncJobType.PUBLICATION_EXPORT)
        assertThat(completedJob.progress).isEqualTo(3)
        assertThat(completedJob.total).isEqualTo(3)

        val result = completedJob.result as Map<*, *>
        assertThat(result["format"]).isEqualTo("CSV")
        assertThat(result["recordCount"]).isEqualTo(3)
        assertThat(result["url"]).isNotNull()

        // Verify S3 file content
        val url = result["url"] as String
        val key = extractS3KeyFromUrl(url)
        val csvContent = downloadFromS3(key)

        assertThat(csvContent).contains("code,customer_id,campaign,channel,date")
        assertThat(csvContent).contains("PUBVOUCHER-1")
        assertThat(csvContent).contains("PUBVOUCHER-2")
        assertThat(csvContent).contains("PUBVOUCHER-3")
        assertThat(csvContent).contains("Test Campaign")
        assertThat(csvContent).contains("API")
    }

    @Test
    fun `should export publications to JSON format`() {
        // Given
        val command = PublicationExportCommand(
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
                assertThat(job.status).isEqualTo(AsyncJobStatus.COMPLETED)
            }

        val completedJob = asyncJobRepository.findById(jobId).orElseThrow()
        val result = completedJob.result as Map<*, *>
        assertThat(result["format"]).isEqualTo("JSON")
        assertThat(result["recordCount"]).isEqualTo(3)

        // Verify JSON content
        val url = result["url"] as String
        val key = extractS3KeyFromUrl(url)
        val jsonContent = downloadFromS3(key)

        assertThat(jsonContent).contains("\"code\"")
        assertThat(jsonContent).contains("\"PUBVOUCHER-1\"")
    }

    @Test
    fun `should handle empty export gracefully`() {
        // Given - Clean all publications
        publicationRepository.deleteAll()

        val command = PublicationExportCommand(
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
        assertThat(result["message"]).isEqualTo("No publications found matching the criteria")
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
