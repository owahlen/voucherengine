package org.wahlen.voucherengine.service

import tools.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.operations.SqsTemplate
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.api.dto.request.VoucherBulkUpdateRequest
import org.wahlen.voucherengine.api.dto.request.VoucherImportRequest
import org.wahlen.voucherengine.api.dto.request.VoucherMetadataUpdateRequest
import org.wahlen.voucherengine.api.dto.sqs.BulkUpdateMessage
import org.wahlen.voucherengine.api.dto.sqs.MetadataUpdateMessage
import org.wahlen.voucherengine.api.dto.sqs.VoucherImportMessage
import org.wahlen.voucherengine.api.dto.sqs.VoucherUpdateItem
import org.wahlen.voucherengine.persistence.model.async.AsyncJob
import org.wahlen.voucherengine.persistence.model.async.AsyncJobStatus
import org.wahlen.voucherengine.persistence.model.async.AsyncJobType
import org.wahlen.voucherengine.persistence.repository.AsyncJobRepository
import java.util.UUID

/**
 * Service for publishing async jobs to SQS queue
 */
@Service
class AsyncJobPublisher(
    private val sqsTemplate: SqsTemplate,
    private val asyncJobRepository: AsyncJobRepository,
    private val tenantService: TenantService,
    private val objectMapper: ObjectMapper,
    @Value("\${aws.sqs.queues.async-jobs}") private val queueName: String
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun publishBulkUpdate(
        tenantName: String,
        updates: List<VoucherBulkUpdateRequest>
    ): UUID {
        val tenant = tenantService.requireTenant(tenantName)

        // Create job record
        val job = AsyncJob(
            type = AsyncJobType.BULK_VOUCHER_UPDATE,
            status = AsyncJobStatus.PENDING,
            total = updates.size,
            parameters = mapOf(
                "updateCount" to updates.size
            )
        )
        job.tenant = tenant
        val savedJob = asyncJobRepository.save(job)

        // Send to SQS
        val message = BulkUpdateMessage(
            jobId = savedJob.id!!,
            tenantName = tenantName,
            updates = updates.map { VoucherUpdateItem(it.code, it.metadata) }
        )

        val messageJson = objectMapper.writeValueAsString(message)

        sqsTemplate.send { to ->
            to.queue(queueName)
            to.payload(messageJson)
            to.header("jobId", savedJob.id.toString())
            to.header("jobType", AsyncJobType.BULK_VOUCHER_UPDATE.name)
            to.header("contentType", "application/json")
            to.header("JavaType", BulkUpdateMessage::class.java.name)
        }

        logger.info("Published BULK_VOUCHER_UPDATE job ${savedJob.id} for tenant $tenantName")
        return savedJob.id!!
    }

    @Transactional
    fun publishMetadataUpdate(
        tenantName: String,
        request: VoucherMetadataUpdateRequest
    ): UUID {
        val tenant = tenantService.requireTenant(tenantName)

        val job = AsyncJob(
            type = AsyncJobType.VOUCHER_METADATA_UPDATE,
            status = AsyncJobStatus.PENDING,
            total = request.codes.size,
            parameters = mapOf(
                "codeCount" to request.codes.size,
                "metadata" to request.metadata
            )
        )
        job.tenant = tenant
        val savedJob = asyncJobRepository.save(job)

        val message = MetadataUpdateMessage(
            jobId = savedJob.id!!,
            tenantName = tenantName,
            codes = request.codes,
            metadata = request.metadata
        )

        val messageJson = objectMapper.writeValueAsString(message)

        sqsTemplate.send { to ->
            to.queue(queueName)
            to.payload(messageJson)
            to.header("jobId", savedJob.id.toString())
            to.header("jobType", AsyncJobType.VOUCHER_METADATA_UPDATE.name)
            to.header("contentType", "application/json")
            to.header("JavaType", MetadataUpdateMessage::class.java.name)
        }

        logger.info("Published VOUCHER_METADATA_UPDATE job ${savedJob.id} for tenant $tenantName")
        return savedJob.id!!
    }

    @Transactional
    fun publishVoucherImport(
        tenantName: String,
        request: VoucherImportRequest
    ): UUID {
        val tenant = tenantService.requireTenant(tenantName)

        val job = AsyncJob(
            type = AsyncJobType.VOUCHER_IMPORT,
            status = AsyncJobStatus.PENDING,
            total = request.vouchers.size,
            parameters = mapOf(
                "voucherCount" to request.vouchers.size
            )
        )
        job.tenant = tenant
        val savedJob = asyncJobRepository.save(job)

        // Serialize vouchers to JSON
        val vouchersJson = objectMapper.writeValueAsString(request.vouchers)

        val message = VoucherImportMessage(
            jobId = savedJob.id!!,
            tenantName = tenantName,
            vouchers = vouchersJson
        )

        val messageJson = objectMapper.writeValueAsString(message)

        sqsTemplate.send { to ->
            to.queue(queueName)
            to.payload(messageJson)
            to.header("jobId", savedJob.id.toString())
            to.header("jobType", AsyncJobType.VOUCHER_IMPORT.name)
            to.header("contentType", "application/json")
            to.header("JavaType", VoucherImportMessage::class.java.name)
        }

        logger.info("Published VOUCHER_IMPORT job ${savedJob.id} for tenant $tenantName")
        return savedJob.id!!
    }
}
