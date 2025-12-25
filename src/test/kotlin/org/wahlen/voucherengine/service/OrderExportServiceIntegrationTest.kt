package org.wahlen.voucherengine.service

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.config.S3MockTestConfiguration
import org.wahlen.voucherengine.config.SqsIntegrationTest
import org.wahlen.voucherengine.persistence.model.async.AsyncJobStatus
import org.wahlen.voucherengine.persistence.model.async.AsyncJobType
import org.wahlen.voucherengine.persistence.model.customer.Customer
import org.wahlen.voucherengine.persistence.model.order.Order
import org.wahlen.voucherengine.persistence.model.tenant.Tenant
import org.wahlen.voucherengine.persistence.repository.AsyncJobRepository
import org.wahlen.voucherengine.persistence.repository.CustomerRepository
import org.wahlen.voucherengine.persistence.repository.OrderRepository
import org.wahlen.voucherengine.persistence.repository.TenantRepository
import org.wahlen.voucherengine.service.async.AsyncJobPublisher
import org.wahlen.voucherengine.service.async.command.OrderExportCommand
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import java.time.Duration
import java.util.concurrent.TimeUnit

@SqsIntegrationTest
@Import(S3MockTestConfiguration::class)
class OrderExportServiceIntegrationTest {

    @Autowired
    private lateinit var orderExportService: OrderExportService

    @Autowired
    private lateinit var asyncJobPublisher: AsyncJobPublisher

    @Autowired
    private lateinit var asyncJobRepository: AsyncJobRepository

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Autowired
    private lateinit var customerRepository: CustomerRepository

    @Autowired
    private lateinit var tenantRepository: TenantRepository

    @Autowired
    private lateinit var s3Client: S3Client

    private lateinit var tenant: Tenant
    private lateinit var customer: Customer

    @BeforeEach
    @Transactional
    fun setup() {
        // Clean up
        asyncJobRepository.deleteAll()
        orderRepository.deleteAll()
        customerRepository.deleteAll()
        tenantRepository.deleteAll()

        // Create tenant
        tenant = Tenant(name = "test-export-tenant")
        tenant = tenantRepository.save(tenant)

        // Create customer
        customer = Customer(
            sourceId = "cust-001",
            name = "Test Customer",
            email = "test@example.com",
            tenant = tenant
        )
        customer = customerRepository.save(customer)

        // Create test orders
        repeat(5) { i ->
            val order = Order(
                sourceId = "order-${i + 1}",
                status = "PAID",
                amount = (100 + i * 10).toLong(),
                initialAmount = (100 + i * 10).toLong(),
                discountAmount = 0L,
                customer = customer,
                tenant = tenant
            )
            orderRepository.save(order)
        }
    }

    @Test
    fun `should export orders to CSV format`() {
        // Given
        val command = OrderExportCommand(
            tenantName = tenant.name!!,
            parameters = mapOf("format" to "CSV")
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
        assertThat(completedJob.type).isEqualTo(AsyncJobType.ORDER_EXPORT)
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

        assertThat(csvContent).contains("id,source_id,status,amount")
        assertThat(csvContent).contains("order-1")
        assertThat(csvContent).contains("order-5")
        assertThat(csvContent).contains("PAID")
    }

    @Test
    fun `should export orders to JSON format`() {
        // Given
        val command = OrderExportCommand(
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

        assertThat(jsonContent).contains("\"source_id\"")
        assertThat(jsonContent).contains("\"order-1\"")
        assertThat(jsonContent).contains("\"status\"")
        assertThat(jsonContent).contains("\"PAID\"")
    }

    @Test
    fun `should handle empty export gracefully`() {
        // Given - Clean all orders
        orderRepository.deleteAll()

        val command = OrderExportCommand(
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
        assertThat(result["message"]).isEqualTo("No orders found matching the criteria")
    }

    @Test
    fun `should track progress during export`() {
        // Given - Create more orders to see progress
        repeat(50) { i ->
            val order = Order(
                sourceId = "bulk-order-${i + 1}",
                status = "PAID",
                amount = 100L,
                tenant = tenant
            )
            orderRepository.save(order)
        }

        val command = OrderExportCommand(
            tenantName = tenant.name!!,
            parameters = mapOf("format" to "CSV")
        )

        // When
        val jobId = asyncJobPublisher.publish(command, tenant)

        // Then - Check that progress is updated
        var progressSeen = false
        await()
            .atMost(30, TimeUnit.SECONDS)
            .pollInterval(Duration.ofMillis(100))
            .untilAsserted {
                val job = asyncJobRepository.findById(jobId).orElseThrow()
                
                if (job.status == AsyncJobStatus.IN_PROGRESS && job.progress > 0 && job.progress < job.total) {
                    progressSeen = true
                }
                
                assertThat(job.status).isIn(AsyncJobStatus.COMPLETED, AsyncJobStatus.FAILED)
                if (job.status == AsyncJobStatus.FAILED) {
                    throw AssertionError("Job failed: ${job.result}")
                }
            }

        val completedJob = asyncJobRepository.findById(jobId).orElseThrow()
        assertThat(completedJob.progress).isEqualTo(55) // 5 from setup + 50 bulk
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
