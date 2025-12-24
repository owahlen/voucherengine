package org.wahlen.voucherengine.service.async

import tools.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.api.dto.request.VoucherCreateRequest
import org.wahlen.voucherengine.service.async.command.BulkUpdateCommand
import org.wahlen.voucherengine.service.async.command.MetadataUpdateCommand
import org.wahlen.voucherengine.service.async.command.VoucherImportCommand
import org.wahlen.voucherengine.persistence.model.async.AsyncJobStatus
import org.wahlen.voucherengine.persistence.repository.AsyncJobRepository
import org.wahlen.voucherengine.service.VoucherService
import java.time.Clock
import java.time.Instant

/**
 * Service for processing async voucher operations.
 * 
 * Handler methods run within the transaction started by AsyncJobListener.
 */
@Service
class VoucherCommandService(
    private val asyncJobRepository: AsyncJobRepository,
    private val voucherService: VoucherService,
    private val objectMapper: ObjectMapper,
    private val clock: Clock
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun handleBulkUpdate(command: BulkUpdateCommand) {
        val jobId = command.jobId ?: throw IllegalStateException("Command jobId must be set")
        val job = asyncJobRepository.findById(jobId).orElseThrow {
            IllegalArgumentException("Job not found: $jobId")
        }

        try {
            logger.info("Processing BULK_VOUCHER_UPDATE job ${job.id} for tenant ${command.tenantName}")

            job.status = AsyncJobStatus.IN_PROGRESS
            job.startedAt = Instant.now(clock)
            asyncJobRepository.save(job)

            var successCount = 0
            val failedCodes = mutableListOf<String>()

            command.updates.forEachIndexed { index, update ->
                try {
                    val voucher = voucherService.getByCode(command.tenantName, update.code)
                    if (voucher != null) {
                        voucher.metadata = update.metadata
                        voucherService.save(voucher)
                        successCount++
                    } else {
                        failedCodes.add(update.code)
                    }
                } catch (e: Exception) {
                    logger.error("Failed to update voucher ${update.code}", e)
                    failedCodes.add(update.code)
                }

                // Update progress every 100 items
                if (index > 0 && index % 100 == 0) {
                    job.progress = index + 1
                    asyncJobRepository.save(job)
                }
            }

            job.status = AsyncJobStatus.COMPLETED
            job.progress = command.updates.size
            job.result = mapOf(
                "success_count" to successCount,
                "failure_count" to failedCodes.size,
                "failed_codes" to failedCodes
            )
            job.completedAt = Instant.now(clock)
            asyncJobRepository.save(job)

            logger.info("Completed job ${job.id}: $successCount successes, ${failedCodes.size} failures")

        } catch (e: Exception) {
            logger.error("Job ${job.id} failed", e)

            job.status = AsyncJobStatus.FAILED
            job.errorMessage = e.message
            job.completedAt = Instant.now(clock)
            asyncJobRepository.save(job)

            // Re-throw to trigger SQS retry/DLQ
            throw e
        }
    }

    @Transactional
    fun handleMetadataUpdate(command: MetadataUpdateCommand) {
        val jobId = command.jobId ?: throw IllegalStateException("Command jobId must be set")
        val job = asyncJobRepository.findById(jobId).orElseThrow {
            IllegalArgumentException("Job not found: $jobId")
        }

        try {
            logger.info("Processing VOUCHER_METADATA_UPDATE job ${job.id} for tenant ${command.tenantName}")

            job.status = AsyncJobStatus.IN_PROGRESS
            job.startedAt = Instant.now(clock)
            asyncJobRepository.save(job)

            var successCount = 0
            val failedCodes = mutableListOf<String>()

            command.codes.forEachIndexed { index, code ->
                try {
                    val voucher = voucherService.getByCode(command.tenantName, code)
                    if (voucher != null) {
                        // Merge metadata
                        val currentMetadata = voucher.metadata?.toMutableMap() ?: mutableMapOf()
                        currentMetadata.putAll(command.metadata)
                        voucher.metadata = currentMetadata
                        voucherService.save(voucher)
                        successCount++
                    } else {
                        failedCodes.add(code)
                    }
                } catch (e: Exception) {
                    logger.error("Failed to update metadata for voucher $code", e)
                    failedCodes.add(code)
                }

                if (index > 0 && index % 100 == 0) {
                    job.progress = index + 1
                    asyncJobRepository.save(job)
                }
            }

            job.status = AsyncJobStatus.COMPLETED
            job.progress = command.codes.size
            job.result = mapOf(
                "success_count" to successCount,
                "failure_count" to failedCodes.size,
                "failed_codes" to failedCodes
            )
            job.completedAt = Instant.now(clock)
            asyncJobRepository.save(job)

            logger.info("Completed job ${job.id}: $successCount successes, ${failedCodes.size} failures")

        } catch (e: Exception) {
            logger.error("Job ${job.id} failed", e)

            job.status = AsyncJobStatus.FAILED
            job.errorMessage = e.message
            job.completedAt = Instant.now(clock)
            asyncJobRepository.save(job)

            throw e
        }
    }

    @Transactional
    fun handleVoucherImport(command: VoucherImportCommand) {
        val jobId = command.jobId ?: throw IllegalStateException("Command jobId must be set")
        val job = asyncJobRepository.findById(jobId).orElseThrow {
            IllegalArgumentException("Job not found: $jobId")
        }

        try {
            logger.info("Processing VOUCHER_IMPORT job ${job.id} for tenant ${command.tenantName}")

            job.status = AsyncJobStatus.IN_PROGRESS
            job.startedAt = Instant.now(clock)
            asyncJobRepository.save(job)

            // Deserialize vouchers from JSON
            val vouchers: List<VoucherCreateRequest> = objectMapper.readValue(
                command.vouchers,
                objectMapper.typeFactory.constructCollectionType(List::class.java, VoucherCreateRequest::class.java)
            )

            var successCount = 0
            val failedCodes = mutableListOf<String>()

            vouchers.forEachIndexed { index, voucherRequest ->
                try {
                    voucherService.createVoucher(command.tenantName, voucherRequest)
                    successCount++
                } catch (e: Exception) {
                    logger.error("Failed to import voucher ${voucherRequest.code}", e)
                    failedCodes.add(voucherRequest.code ?: "unknown")
                }

                if (index > 0 && index % 100 == 0) {
                    job.progress = index + 1
                    asyncJobRepository.save(job)
                }
            }

            job.status = AsyncJobStatus.COMPLETED
            job.progress = vouchers.size
            job.result = mapOf(
                "success_count" to successCount,
                "failure_count" to failedCodes.size,
                "failed_codes" to failedCodes
            )
            job.completedAt = Instant.now(clock)
            asyncJobRepository.save(job)

            logger.info("Completed job ${job.id}: $successCount successes, ${failedCodes.size} failures")

        } catch (e: Exception) {
            logger.error("Job ${job.id} failed", e)

            job.status = AsyncJobStatus.FAILED
            job.errorMessage = e.message
            job.completedAt = Instant.now(clock)
            asyncJobRepository.save(job)

            throw e
        }
    }

    @Transactional
    fun handleCampaignVoucherGeneration(command: org.wahlen.voucherengine.service.async.command.CampaignVoucherGenerationCommand) {
        val jobId = command.jobId ?: throw IllegalStateException("Command jobId must be set")
        val job = asyncJobRepository.findById(jobId).orElseThrow {
            IllegalArgumentException("Job not found: $jobId")
        }

        try {
            logger.info("Processing CAMPAIGN_VOUCHER_GENERATION job ${job.id} for tenant ${command.tenantName}")

            job.status = AsyncJobStatus.IN_PROGRESS
            job.startedAt = Instant.now(clock)
            asyncJobRepository.save(job)

            var successCount = 0
            val failedIndices = mutableListOf<Int>()

            repeat(command.count) { index ->
                try {
                    // Create voucher with campaign_id from command
                    val voucherRequest = command.voucherTemplate.copy(campaign_id = command.campaignId)
                    voucherService.createVoucher(command.tenantName, voucherRequest)
                    successCount++
                } catch (e: Exception) {
                    logger.error("Failed to generate voucher ${index + 1} of ${command.count}", e)
                    failedIndices.add(index + 1)
                }

                if (index > 0 && index % 100 == 0) {
                    job.progress = index + 1
                    asyncJobRepository.save(job)
                }
            }

            job.status = AsyncJobStatus.COMPLETED
            job.progress = command.count
            job.result = mapOf(
                "success_count" to successCount,
                "failure_count" to failedIndices.size,
                "failed_indices" to failedIndices
            )
            job.completedAt = Instant.now(clock)
            asyncJobRepository.save(job)

            logger.info("CAMPAIGN_VOUCHER_GENERATION job ${job.id} completed: ${successCount}/${command.count} succeeded")

        } catch (e: Exception) {
            logger.error("CAMPAIGN_VOUCHER_GENERATION job ${job.id} failed", e)
            job.status = AsyncJobStatus.FAILED
            job.errorMessage = e.message
            job.completedAt = Instant.now(clock)
            asyncJobRepository.save(job)

            throw e
        }
    }
}
