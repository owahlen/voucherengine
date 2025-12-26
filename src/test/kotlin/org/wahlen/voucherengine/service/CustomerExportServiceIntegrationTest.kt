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
import org.wahlen.voucherengine.persistence.model.customer.Customer
import org.wahlen.voucherengine.persistence.model.order.Order
import org.wahlen.voucherengine.persistence.model.redemption.Redemption
import org.wahlen.voucherengine.persistence.model.redemption.RedemptionResult
import org.wahlen.voucherengine.persistence.model.redemption.RedemptionStatus
import org.wahlen.voucherengine.persistence.model.tenant.Tenant
import org.wahlen.voucherengine.persistence.model.voucher.Voucher
import org.wahlen.voucherengine.persistence.model.voucher.VoucherType
import org.wahlen.voucherengine.persistence.model.campaign.Campaign
import org.wahlen.voucherengine.persistence.repository.*
import org.wahlen.voucherengine.service.async.AsyncJobPublisher
import org.wahlen.voucherengine.service.async.command.CustomerExportCommand
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import java.time.Duration
import java.util.concurrent.TimeUnit

@S3IntegrationTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CustomerExportServiceIntegrationTest {

    @Autowired
    private lateinit var customerExportService: CustomerExportService

    @Autowired
    private lateinit var asyncJobPublisher: AsyncJobPublisher

    @Autowired
    private lateinit var asyncJobRepository: AsyncJobRepository

    @Autowired
    private lateinit var customerRepository: CustomerRepository

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Autowired
    private lateinit var redemptionRepository: RedemptionRepository

    @Autowired
    private lateinit var voucherRepository: VoucherRepository

    @Autowired
    private lateinit var campaignRepository: CampaignRepository

    @Autowired
    private lateinit var tenantRepository: TenantRepository

    @Autowired
    private lateinit var exportRepository: ExportRepository

    @Autowired
    private lateinit var s3Client: S3Client

    private lateinit var tenant: Tenant

    @BeforeEach
    fun setup() {
        // Clean up THIS tenant's data only
        val tenantName = "test-customer-export-tenant"
        
        redemptionRepository.findAllByTenantName(tenantName).forEach { redemptionRepository.delete(it) }
        orderRepository.findAllByTenantName(tenantName).forEach { orderRepository.delete(it) }
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
        val campaign = Campaign(
            name = "Test Campaign",
            tenant = tenant
        )
        campaignRepository.save(campaign)

        // Create customers with orders and redemptions
        repeat(2) { i ->
            val customer = Customer(
                sourceId = "customer-00${i + 1}",
                name = "Customer ${i + 1}",
                email = "customer${i + 1}@example.com",
                phone = "+1234567890",
                tenant = tenant
            )
            customerRepository.save(customer)

            // Create orders for this customer
            repeat(2) { j ->
                val order = Order(
                    sourceId = "order-${i}-${j}",
                    status = "PAID",
                    amount = ((i + 1) * 100 + j * 10).toLong(),
                    initialAmount = ((i + 1) * 100 + j * 10).toLong(),
                    discountAmount = 0L,
                    customer = customer,
                    tenant = tenant
                )
                orderRepository.save(order)
            }

            // Create voucher and redemptions
            val voucher = Voucher(
                code = "VOUCHER-${i + 1}",
                campaign = campaign,
                active = true,
                type = VoucherType.DISCOUNT_VOUCHER,
                tenant = tenant
            )
            voucherRepository.save(voucher)

            // Successful redemption
            val redemption1 = Redemption(
                voucher = voucher,
                customer = customer,
                result = RedemptionResult.SUCCESS,
                status = RedemptionStatus.SUCCEEDED,
                tenant = tenant
            )
            redemptionRepository.save(redemption1)

            // Failed redemption
            if (i == 0) {
                val redemption2 = Redemption(
                    voucher = voucher,
                    customer = customer,
                    result = RedemptionResult.FAILURE,
                    status = RedemptionStatus.FAILED,
                    tenant = tenant
                )
                redemptionRepository.save(redemption2)
            }
        }
    }

    @Test
    fun `should export customers with aggregates to CSV format`() {
        // Given - Request with aggregate fields
        val command = CustomerExportCommand(
            tenantName = tenant.name!!,
            parameters = mapOf(
                "format" to "CSV",
                "fields" to listOf(
                    "name", "email", "source_id",
                    "orders_total_count", "orders_total_amount",
                    "redemptions_total_redeemed", "redemptions_total_succeeded", "redemptions_total_failed"
                )
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
        assertThat(completedJob.type).isEqualTo(AsyncJobType.CUSTOMER_EXPORT)
        assertThat(completedJob.progress).isEqualTo(2) // 2 customers
        assertThat(completedJob.total).isEqualTo(2)

        val result = completedJob.result as Map<*, *>
        assertThat(result["format"]).isEqualTo("CSV")
        assertThat(result["recordCount"]).isEqualTo(2)
        assertThat(result["url"]).isNotNull()

        // Verify S3 file content
        val url = result["url"] as String
        val key = extractS3KeyFromUrl(url)
        val csvContent = downloadFromS3(key)

        // Check headers
        assertThat(csvContent).contains("name")
        assertThat(csvContent).contains("email")
        assertThat(csvContent).contains("orders_total_count")
        
        // Check customer data exists
        assertThat(csvContent).contains("Customer 1")
        assertThat(csvContent).contains("Customer 2")
        
        // Check that we have order count data (2 orders per customer)
        assertThat(csvContent).contains("2")
        
        // Check redemption data exists
        assertThat(csvContent).containsAnyOf("1", "2") // redemption counts
    }

    @Test
    fun `should export customers to JSON format`() {
        // Given
        val command = CustomerExportCommand(
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
        assertThat(result["recordCount"]).isEqualTo(2)

        // Verify JSON content
        val url = result["url"] as String
        val key = extractS3KeyFromUrl(url)
        val jsonContent = downloadFromS3(key)

        assertThat(jsonContent).contains("\"name\"")
        assertThat(jsonContent).contains("\"Customer 1\"")
        assertThat(jsonContent).contains("\"source_id\"")
    }

    @Test
    fun `should handle empty export gracefully`() {
        // Given - Clean all data for this tenant in proper order
        redemptionRepository.findAllByTenantName(tenant.name!!).forEach { redemptionRepository.delete(it) }
        orderRepository.findAllByTenantName(tenant.name!!).forEach { orderRepository.delete(it) }
        voucherRepository.findAllByTenantName(tenant.name!!).forEach { voucherRepository.delete(it) }
        customerRepository.findAllByTenantName(tenant.name!!).forEach { customerRepository.delete(it) }

        val command = CustomerExportCommand(
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
        assertThat(result["message"]).isEqualTo("No customers found matching the criteria")
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
