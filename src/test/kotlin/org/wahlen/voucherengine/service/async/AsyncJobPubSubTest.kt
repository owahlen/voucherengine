package org.wahlen.voucherengine.service.async

import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.api.dto.request.VoucherBulkUpdateRequest
import org.wahlen.voucherengine.api.dto.request.VoucherCreateRequest
import org.wahlen.voucherengine.api.dto.request.VoucherImportRequest
import org.wahlen.voucherengine.api.dto.request.VoucherMetadataUpdateRequest
import org.wahlen.voucherengine.config.SqsIntegrationTest
import org.wahlen.voucherengine.persistence.model.tenant.Tenant
import org.wahlen.voucherengine.persistence.repository.AsyncJobRepository
import org.wahlen.voucherengine.persistence.repository.TenantRepository
import org.wahlen.voucherengine.service.VoucherService
import org.wahlen.voucherengine.service.async.command.BulkUpdateCommand
import org.wahlen.voucherengine.service.async.command.MetadataUpdateCommand
import org.wahlen.voucherengine.service.async.command.VoucherImportCommand
import tools.jackson.databind.ObjectMapper
import java.time.Clock
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pub/Sub integration test for async jobs using real ElasticMQ/SQS.
 * 
 * Tests the full flow:
 * 1. AsyncJobPublisher publishes commands to SQS
 * 2. AsyncJobListener receives and deserializes commands
 * 3. VoucherAsyncService processes commands
 */
@SqsIntegrationTest
@Import(AsyncJobPubSubTest.TestConfig::class)
class AsyncJobPubSubTest @Autowired constructor(
    private val voucherJobService: VoucherJobService,
    private val tenantRepository: TenantRepository,
    private val testTracker: TestExecutionTracker
) {
    private val tenantName = "test-tenant"

    @TestConfiguration
    class TestConfig {
        @Bean
        fun testExecutionTracker() = TestExecutionTracker()

        @Bean
        @Primary
        fun testVoucherAsyncService(
            asyncJobRepository: AsyncJobRepository,
            voucherService: VoucherService,
            objectMapper: ObjectMapper,
            clock: Clock,
            tracker: TestExecutionTracker
        ): VoucherCommandService = TrackingCommandService(
            asyncJobRepository, voucherService, objectMapper, clock, tracker
        )
    }

    class TestExecutionTracker {
        val executedCommands = mutableListOf<Pair<String, UUID>>()
        var shouldThrowException = false

        fun reset() {
            executedCommands.clear()
            shouldThrowException = false
        }

        fun recordExecution(commandType: String, jobId: UUID) {
            executedCommands.add(commandType to jobId)
            if (shouldThrowException) {
                throw RuntimeException("Simulated execution failure")
            }
        }
    }

    class TrackingCommandService(
        asyncJobRepository: AsyncJobRepository,
        voucherService: VoucherService,
        objectMapper: ObjectMapper,
        clock: Clock,
        private val tracker: TestExecutionTracker
    ) : VoucherCommandService(asyncJobRepository, voucherService, objectMapper, clock) {
        
        @Transactional
        override fun handleBulkUpdate(command: BulkUpdateCommand) {
            tracker.recordExecution("BulkUpdateCommand", command.jobId!!)
        }

        @Transactional
        override fun handleMetadataUpdate(command: MetadataUpdateCommand) {
            tracker.recordExecution("MetadataUpdateCommand", command.jobId!!)
        }

        @Transactional
        override fun handleVoucherImport(command: VoucherImportCommand) {
            tracker.recordExecution("VoucherImportCommand", command.jobId!!)
        }

        @Transactional
        override fun handleCampaignVoucherGeneration(command: org.wahlen.voucherengine.service.async.command.CampaignVoucherGenerationCommand) {
            tracker.recordExecution("CampaignVoucherGenerationCommand", command.jobId!!)
        }
    }

    @BeforeEach
    fun setup() {
        testTracker.reset()
        if (tenantRepository.findByName(tenantName) == null) {
            tenantRepository.save(Tenant(name = tenantName))
        }
    }

    @Test
    fun `should publish and process BulkUpdateCommand`() {
        val updates = listOf(VoucherBulkUpdateRequest(code = "CODE1", metadata = mapOf("key" to "value")))
        val jobId = voucherJobService.publishBulkUpdate(tenantName, updates)

        await.atMost(5, TimeUnit.SECONDS) untilAsserted {
            assertEquals(1, testTracker.executedCommands.size)
            assertEquals("BulkUpdateCommand" to jobId, testTracker.executedCommands[0])
        }
    }

    @Test
    fun `should publish and process MetadataUpdateCommand`() {
        val request = VoucherMetadataUpdateRequest(codes = listOf("CODE1", "CODE2"), metadata = mapOf("updated" to "true"))
        val jobId = voucherJobService.publishMetadataUpdate(tenantName, request)

        await.atMost(5, TimeUnit.SECONDS) untilAsserted {
            assertEquals(1, testTracker.executedCommands.size)
            assertEquals("MetadataUpdateCommand" to jobId, testTracker.executedCommands[0])
        }
    }

    @Test
    fun `should publish and process VoucherImportCommand`() {
        val request = VoucherImportRequest(vouchers = listOf(VoucherCreateRequest(code = "IMPORT1", type = "DISCOUNT_VOUCHER")))
        val jobId = voucherJobService.publishVoucherImport(tenantName, request)

        await.atMost(5, TimeUnit.SECONDS) untilAsserted {
            assertEquals(1, testTracker.executedCommands.size)
            assertEquals("VoucherImportCommand" to jobId, testTracker.executedCommands[0])
        }
    }

    @Test
    fun `should handle execution exceptions and allow SQS retry`() {
        testTracker.shouldThrowException = true
        val updates = listOf(VoucherBulkUpdateRequest(code = "CODE1", metadata = emptyMap()))
        val jobId = voucherJobService.publishBulkUpdate(tenantName, updates)

        await.atMost(5, TimeUnit.SECONDS) untilAsserted {
            assertTrue(testTracker.executedCommands.size >= 1)
            assertEquals("BulkUpdateCommand" to jobId, testTracker.executedCommands[0])
        }
    }

    @Test
    fun `should process multiple commands in sequence`() {
        val jobId1 = voucherJobService.publishBulkUpdate(tenantName, listOf(VoucherBulkUpdateRequest(code = "C1", metadata = emptyMap())))
        val jobId2 = voucherJobService.publishMetadataUpdate(tenantName, VoucherMetadataUpdateRequest(codes = listOf("C2"), metadata = emptyMap()))
        val jobId3 = voucherJobService.publishVoucherImport(tenantName, VoucherImportRequest(vouchers = listOf(VoucherCreateRequest(code = "C3", type = "DISCOUNT_VOUCHER"))))

        await.atMost(10, TimeUnit.SECONDS) untilAsserted {
            assertEquals(3, testTracker.executedCommands.size)
            assertTrue(testTracker.executedCommands.any { it.second == jobId1 })
            assertTrue(testTracker.executedCommands.any { it.second == jobId2 })
            assertTrue(testTracker.executedCommands.any { it.second == jobId3 })
        }
    }
}
