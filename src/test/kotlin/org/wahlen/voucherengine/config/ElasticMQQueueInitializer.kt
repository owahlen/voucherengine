package org.wahlen.voucherengine.config

import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest

/**
 * Initializes queues in ElasticMQ after SqsAsyncClient is available.
 *
 * This class is instantiated as a Spring bean in ElasticMQTestConfiguration
 * and creates all configured queues when the SqsAsyncClient becomes available.
 *
 * Queues are read from SqsQueueProperties (aws.sqs.queues.* configuration).
 */
class ElasticMQQueueInitializer(
    private val sqsAsyncClient: SqsAsyncClient,
    private val sqsQueueProperties: SqsQueueProperties
) {
    private val logger = LoggerFactory.getLogger(ElasticMQQueueInitializer::class.java)

    init {
        createQueues()
    }

    private fun createQueues() {
        val queueNames = sqsQueueProperties.getAllQueueNames()

        if (queueNames.isEmpty()) {
            logger.warn("No queues configured in aws.sqs.queues")
            return
        }

        queueNames.forEach { queueName ->
            try {
                sqsAsyncClient.createQueue(
                    CreateQueueRequest.builder()
                        .queueName(queueName)
                        .build()
                ).join()
                logger.info("Created queue: $queueName")
            } catch (e: Exception) {
                logger.warn("Could not create queue $queueName: ${e.message}")
            }
        }
    }
}

