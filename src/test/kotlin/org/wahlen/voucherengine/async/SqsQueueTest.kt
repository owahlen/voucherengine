package org.wahlen.voucherengine.async

import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.wahlen.voucherengine.config.SqsIntegrationTest
import org.wahlen.voucherengine.service.async.AsyncJobListener
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


/**
 * Diagnostic test to understand ElasticMQ
 */
@SqsIntegrationTest
@Import(SqsQueueTestConfiguration::class)
class SqsQueueTest @Autowired constructor(
    private val sqsAsyncClient: SqsAsyncClient,
    private val sqsQueueListener: SqsQueueListener
) {
    private val queueName = "async-jobs"

    // Disable the AsyncJobListener as the test sends a dummy message that must not be proceeded
    @MockitoBean
    lateinit var asyncJobListener: AsyncJobListener

    @BeforeEach
    fun setup() {
        sqsQueueListener.reset()
    }

    @Test
    fun `ElasticMQ should be running and accessible`() {
        // Try to get queue URL - this will fail if ElasticMQ isn't running
        val response = sqsAsyncClient.getQueueUrl(
            GetQueueUrlRequest.builder()
                .queueName(queueName)
                .build()
        ).join()

        assertNotNull(response.queueUrl())
        assertTrue(response.queueUrl().contains(queueName))
    }

    @Test
    fun `should be able to send and receive messages from ElasticMQ`() {
        val queueUrl = sqsAsyncClient.getQueueUrl(
            GetQueueUrlRequest.builder()
                .queueName(queueName)
                .build()
        ).join().queueUrl()

        // Send a test message
        val testMessage = """{"test": "message"}"""
        sqsAsyncClient.sendMessage(
            SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(testMessage)
                .messageAttributes(
                    mapOf(
                        "SenderId" to MessageAttributeValue.builder()
                            .dataType("String")
                            .stringValue("test-sender")
                            .build()
                    )
                )
                .build()
        ).join()

        // Wait for the message to be received by the listener and validate
        await.atMost(5, TimeUnit.SECONDS) untilAsserted {
            assertEquals(1, sqsQueueListener.receivedMessages.size)
            assertEquals(testMessage, sqsQueueListener.receivedMessages[0].body)
            assertEquals("test-sender", sqsQueueListener.receivedMessages[0].senderId)
        }
    }

}
