package org.wahlen.voucherengine.service.async

import tools.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.service.async.command.AsyncJobCommand

/**
 * Service for listening to and processing async job commands from SQS
 * 
 * Uses Jackson polymorphic deserialization with jobType as discriminator.
 * Each message is processed in its own transaction (REQUIRES_NEW) to ensure
 * isolation between different SQS messages.
 */
@Service
class AsyncJobListener(
    private val objectMapper: ObjectMapper,
    private val applicationContext: ApplicationContext
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @SqsListener("\${aws.sqs.queues.async-jobs}")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleAsyncJob(message: String) {
        try {
            logger.info("Received SQS message: $message")
            
            // Jackson will use the jobType property to deserialize to the correct subtype
            val command = objectMapper.readValue(message, AsyncJobCommand::class.java)
            
            logger.info("Processing job ${command.jobId} of type ${command::class.simpleName}")
            
            // Execute the command using the polymorphic execute method
            command.execute(applicationContext)
            
        } catch (e: Exception) {
            logger.error("Failed to process SQS message: $message", e)
            // Re-throw to trigger SQS retry/DLQ
            throw e
        }
    }
}
