package org.wahlen.voucherengine.infrastructure

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.wahlen.voucherengine.config.SqsIntegrationTest
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest
import software.amazon.awssdk.services.sqs.model.ListQueuesRequest
import software.amazon.awssdk.services.sqs.model.QueueAttributeName
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration test for ElasticMQ extension and SQS autoconfiguration.
 *
 * Verifies that:
 * - ElasticMqExtension starts successfully
 * - Queues are created automatically
 * - SqsAsyncClient bean is properly configured
 * - Queues are accessible
 */
@SqsIntegrationTest
class ElasticMqIntegrationTest {

    @Autowired
    private lateinit var sqsAsyncClient: SqsAsyncClient

    @Test
    fun `should list queues created on initialization`() {
        // When
        val listResponse = sqsAsyncClient.listQueues(
            ListQueuesRequest.builder().build()
        ).join()

        // Then
        val queueUrls = listResponse.queueUrls()
        assertTrue(queueUrls.isNotEmpty(), "At least one queue should exist")

        // Verify default queue exists
        val hasAsyncJobsQueue = queueUrls.any { it.contains("voucherengine-async-jobs") }
        assertTrue(hasAsyncJobsQueue, "voucherengine-async-jobs queue should be created by default")
    }

    @Test
    fun `should verify queue is accessible`() {
        // Given
        val queueName = "voucherengine-async-jobs"

        // When
        val queueUrl = sqsAsyncClient.getQueueUrl(
            GetQueueUrlRequest.builder()
                .queueName(queueName)
                .build()
        ).join().queueUrl()

        // Then
        assertTrue(queueUrl.contains(queueName), "Queue URL should contain queue name")
        assertTrue(queueUrl.startsWith("http://"), "Queue URL should be HTTP endpoint")
    }

    @Test
    fun `should get queue attributes`() {
        // Given
        val queueName = "voucherengine-async-jobs"
        val queueUrl = sqsAsyncClient.getQueueUrl(
            GetQueueUrlRequest.builder()
                .queueName(queueName)
                .build()
        ).join().queueUrl()

        // When
        val attributes = sqsAsyncClient.getQueueAttributes(
            GetQueueAttributesRequest.builder()
                .queueUrl(queueUrl)
                .attributeNames(QueueAttributeName.ALL)
                .build()
        ).join().attributes()

        // Then
        assertNotNull(attributes, "Queue attributes should be available")
        assertTrue(attributes.containsKey(QueueAttributeName.QUEUE_ARN), "Queue should have ARN attribute")
    }
}