package org.wahlen.voucherengine.service.async

import io.awspring.cloud.sqs.operations.SqsTemplate
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.persistence.model.tenant.Tenant
import org.wahlen.voucherengine.persistence.repository.AsyncJobRepository
import org.wahlen.voucherengine.service.async.command.AsyncJobCommand
import tools.jackson.databind.ObjectMapper
import java.util.UUID

/**
 * Generic publisher for async job commands.
 * 
 * This service is agnostic to specific command types and provides a generic
 * publish mechanism that:
 * 1. Lets the command create its AsyncJob entity
 * 2. Persists the job to the database
 * 3. Serializes and sends the command to SQS
 */
@Service
class AsyncJobPublisher(
    private val sqsTemplate: SqsTemplate,
    private val asyncJobRepository: AsyncJobRepository,
    private val objectMapper: ObjectMapper,
    @Value("\${aws.sqs.queues.async-jobs}") private val queueName: String
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Publish a command to SQS and persist the job record.
     * 
     * The flow is:
     * 1. Command creates AsyncJob entity (without ID)
     * 2. Hibernate generates ID when persisting
     * 3. Generated ID is set back into the command
     * 4. Command with ID is published to SQS
     * 
     * @param command The command to publish
     * @param tenant The tenant owning this job
     * @return The job ID
     */
    @Transactional
    fun publish(command: AsyncJobCommand, tenant: Tenant): UUID {
        // Create the AsyncJob entity (without ID)
        val job = command.toAsyncJob(tenant)
        
        // Save - Hibernate generates the ID
        val savedJob = asyncJobRepository.save(job)
        
        // Set the generated ID back into the command
        command.jobId = savedJob.id!!
        
        // Serialize command with ID to JSON
        val commandJson = objectMapper.writeValueAsString(command)
        
        // Send to SQS
        sqsTemplate.send { to ->
            to.queue(queueName)
            to.payload(commandJson)
            to.header("contentType", "application/json")
        }
        
        logger.info("Published ${job.type} job ${savedJob.id} for tenant ${tenant.name}")
        return savedJob.id!!
    }
}
