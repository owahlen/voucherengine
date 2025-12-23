package org.wahlen.voucherengine.service

import tools.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.LoggerFactory
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.api.dto.request.VoucherCreateRequest
import org.wahlen.voucherengine.api.dto.sqs.BulkUpdateMessage
import org.wahlen.voucherengine.api.dto.sqs.MetadataUpdateMessage
import org.wahlen.voucherengine.api.dto.sqs.VoucherImportMessage
import org.wahlen.voucherengine.persistence.model.async.AsyncJobStatus
import org.wahlen.voucherengine.persistence.model.async.AsyncJobType
import org.wahlen.voucherengine.persistence.repository.AsyncJobRepository
import java.time.Clock
import java.time.Instant

/**
 * Service for listening to and processing async jobs from SQS
 * 
 * Uses a single listener method that dispatches to specific handlers based on jobType header.
 * This avoids issues with multiple listeners on the same queue.
 */
@Service
class AsyncJobListener(
    private val asyncJobRepository: AsyncJobRepository,
    private val voucherService: VoucherService,
    private val objectMapper: ObjectMapper,
    private val clock: Clock
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @SqsListener("\${aws.sqs.queues.async-jobs}")
    fun handleAsyncJob(
        message: String,
        @Header("jobType", required = false) jobType: String?
    ) {
        try {
            logger.info("Received SQS message with jobType: $jobType")
            
            when (jobType) {
                AsyncJobType.BULK_VOUCHER_UPDATE.name -> {
                    val bulkMessage = objectMapper.readValue(message, BulkUpdateMessage::class.java)
                    handleBulkUpdate(bulkMessage)
                }
                AsyncJobType.VOUCHER_METADATA_UPDATE.name -> {
                    val metadataMessage = objectMapper.readValue(message, MetadataUpdateMessage::class.java)
                    handleMetadataUpdate(metadataMessage)
                }
                AsyncJobType.VOUCHER_IMPORT.name -> {
                    val importMessage = objectMapper.readValue(message, VoucherImportMessage::class.java)
                    handleVoucherImport(importMessage)
                }
                else -> {
                    logger.error("Unknown job type: $jobType, message: $message")
                    throw IllegalArgumentException("Unknown job type: $jobType")
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to process SQS message with jobType: $jobType", e)
            // Re-throw to trigger SQS retry/DLQ
            throw e
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleBulkUpdate(message: BulkUpdateMessage) {
        val job = asyncJobRepository.findById(message.jobId).orElseThrow {
            IllegalArgumentException("Job not found: ${message.jobId}")
        }

        try {
            logger.info("Processing BULK_VOUCHER_UPDATE job ${job.id} for tenant ${message.tenantName}")

            job.status = AsyncJobStatus.IN_PROGRESS
            job.startedAt = Instant.now(clock)
            asyncJobRepository.save(job)

            var successCount = 0
            val failedCodes = mutableListOf<String>()

            message.updates.forEachIndexed { index, update ->
                try {
                    val voucher = voucherService.getByCode(message.tenantName, update.code)
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
            job.progress = message.updates.size
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleMetadataUpdate(message: MetadataUpdateMessage) {
        val job = asyncJobRepository.findById(message.jobId).orElseThrow {
            IllegalArgumentException("Job not found: ${message.jobId}")
        }

        try {
            logger.info("Processing VOUCHER_METADATA_UPDATE job ${job.id} for tenant ${message.tenantName}")

            job.status = AsyncJobStatus.IN_PROGRESS
            job.startedAt = Instant.now(clock)
            asyncJobRepository.save(job)

            var successCount = 0
            val failedCodes = mutableListOf<String>()

            message.codes.forEachIndexed { index, code ->
                try {
                    val voucher = voucherService.getByCode(message.tenantName, code)
                    if (voucher != null) {
                        // Merge metadata
                        val currentMetadata = voucher.metadata?.toMutableMap() ?: mutableMapOf()
                        currentMetadata.putAll(message.metadata)
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
            job.progress = message.codes.size
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleVoucherImport(message: VoucherImportMessage) {
        val job = asyncJobRepository.findById(message.jobId).orElseThrow {
            IllegalArgumentException("Job not found: ${message.jobId}")
        }

        try {
            logger.info("Processing VOUCHER_IMPORT job ${job.id} for tenant ${message.tenantName}")

            job.status = AsyncJobStatus.IN_PROGRESS
            job.startedAt = Instant.now(clock)
            asyncJobRepository.save(job)

            // Deserialize vouchers from JSON
            val vouchers: List<VoucherCreateRequest> = objectMapper.readValue(
                message.vouchers,
                objectMapper.typeFactory.constructCollectionType(List::class.java, VoucherCreateRequest::class.java)
            )

            var successCount = 0
            val failedCodes = mutableListOf<String>()

            vouchers.forEachIndexed { index, voucherRequest ->
                try {
                    voucherService.createVoucher(message.tenantName, voucherRequest)
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
}
